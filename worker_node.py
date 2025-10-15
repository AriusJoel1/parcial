#!/usr/bin/env python

import socket, threading, json, os, sys, time

CENTRAL_HOST = "localhost"
CENTRAL_PORT = 9000
WORKER_ID = sys.argv[1] if len(sys.argv) > 1 else "w1"
STORAGE = f"worker_{WORKER_ID}_data.json"

lock = threading.Lock()
loan_counter = 0

if not os.path.exists(STORAGE):
    with open(STORAGE, "w") as f:
        json.dump({"accounts": {}, "transactions": {}, "loans": {}, "loan_counter": 0}, f)

def load():
    with lock:
        with open(STORAGE, "r") as f:
            return json.load(f)

def save(data):
    with lock:
        with open(STORAGE, "w") as f:
            json.dump(data, f, indent=2)

def handle_command(cmd):
    global loan_counter
    parts = cmd.strip().split("|")
    op = parts[0]
    data = load()

    if op == "CREATE_ACCOUNT":
        aid = int(parts[1]); bal = float(parts[2])
        data["accounts"][str(aid)] = {"balance": bal, "loans": [], "id": aid, "id_cliente": aid}
        save(data)
        return "OK"

    elif op == "CONSULTAR_CUENTA":
        aid = parts[1]
        acc = data["accounts"].get(aid)
        if acc: return "OK|"+json.dumps(acc)
        return "ERROR|NoExiste"

    elif op == "DEBIT":
        aid = parts[1]; amt = float(parts[2])
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"
        if acc["balance"] < amt: return "ERROR|SaldoInsuficiente"
        acc["balance"] -= amt
        data["transactions"].setdefault(aid, []).append({"type":"debit","amount":amt})
        save(data)
        return "OK"

    elif op == "CREDIT":
        aid = parts[1]; amt = float(parts[2])
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"
        acc["balance"] += amt
        data["transactions"].setdefault(aid, []).append({"type":"credit","amount":amt})
        save(data)
        return "OK"

    elif op == "RECORD_TX":
        src = parts[1]; dst = parts[2]; amt = float(parts[3])
        data["transactions"].setdefault(src, []).append({"type":"transfer_out","to":dst,"amount":amt})
        data["transactions"].setdefault(dst, []).append({"type":"transfer_in","from":src,"amount":amt})
        save(data)
        return "OK"

    elif op == "CREAR_PRESTAMO":
        aid = parts[1]; amount = float(parts[2]); pending = float(parts[3])
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"

        loan_counter = data.get("loan_counter", 0) + 1
        data["loan_counter"] = loan_counter

        loan = {
            "id": loan_counter,
            "id_cliente": acc.get("id_cliente", int(aid)),
            "monto": amount,
            "monto_pendiente": pending,
            "estado": "Activo",
            "fecha_solicitud": time.strftime("%Y-%m-%d %H:%M:%S")
        }

        data["loans"].setdefault(aid, []).append(loan)
        acc.setdefault("loans", []).append(loan_counter)
        save(data)
        return f"OK|LoanID:{loan_counter}"

    elif op == "PAGAR_PRESTAMO":
        aid = parts[1]; loan_id = int(parts[2]); amount = float(parts[3])
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"

        loans = data["loans"].get(aid, [])
        loan = next((l for l in loans if l["id"] == loan_id), None)
        if not loan: return "ERROR|PrestamoNoExiste"

        if loan["monto_pendiente"] < amount:
            return "ERROR|MontoExcedeMontoPendiente"

        loan["monto_pendiente"] -= amount
        if loan["monto_pendiente"] <= 0:
            loan["estado"] = "Cancelado"
            loan["monto_pendiente"] = 0

        save(data)
        return f"OK|MontoRestante:{loan['monto_pendiente']}"

    elif op == "ESTADO_PAGO_PRESTAMO":
        aid = parts[1]
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"

        loans = data["loans"].get(aid, [])
        loan_details = []
        for loan in loans:
            monto_pagado = loan["monto"] - loan["monto_pendiente"]
            loan_details.append({
                "id_prestamo": loan["id"],
                "monto_total": loan["monto"],
                "monto_pagado": monto_pagado,
                "monto_pendiente": loan["monto_pendiente"],
                "estado": loan["estado"]
            })

        return "OK|"+json.dumps(loan_details)

    elif op == "ARQUEO":
        total_balance = sum(acc["balance"] for acc in data["accounts"].values())
        total_accounts = len(data["accounts"])
        return f"OK|{total_balance}|{total_accounts}"

    else:
        return "ERROR|UnknownOp"

def listen_to_server(sock):
    try:
        buf = b""
        while True:
            data = sock.recv(4096)
            if not data:
                break
            buf += data
            while b"\n" in buf:
                line, buf = buf.split(b"\n", 1)
                line = line.decode().strip()
                if line == "":
                    continue
                resp = handle_command(line)
                sock.sendall((resp + "\n").encode())
    except Exception as e:
        print("Worker error:", e)
    finally:
        try:
            sock.close()
        except:
            pass

def main():
    while True:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((CENTRAL_HOST, CENTRAL_PORT))
            sock.sendall(f"WORKER|{WORKER_ID}\n".encode())
            print(f"Worker {WORKER_ID} connected to server {CENTRAL_HOST}:{CENTRAL_PORT}")
            listen_to_server(sock)
            print(f"Worker {WORKER_ID} disconnected normally, exiting.")
            break
        except Exception as e:
            print(f"Worker {WORKER_ID}: no puedo conectar al servidor aún ({e}). Reintentando en 2s...")
            time.sleep(2)

if __name__ == "__main__":
    main()
