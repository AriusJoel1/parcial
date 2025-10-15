#!/usr/bin/env python

import socket, threading, json, os, sys, time

CENTRAL_HOST = "localhost"
CENTRAL_PORT = 9000
WORKER_ID = sys.argv[1] if len(sys.argv) > 1 else "w1"
STORAGE = f"worker_{WORKER_ID}_data.json"

lock = threading.Lock()
if not os.path.exists(STORAGE):
    with open(STORAGE, "w") as f:
        json.dump({"accounts": {}, "transactions": {}}, f)

def load():
    with lock:
        with open(STORAGE, "r") as f:
            return json.load(f)

def save(data):
    with lock:
        with open(STORAGE, "w") as f:
            json.dump(data, f, indent=2)

def handle_command(cmd):
    parts = cmd.strip().split("|")
    op = parts[0]
    data = load()
    if op == "CREATE_ACCOUNT":
        aid = int(parts[1]); bal = float(parts[2])
        data["accounts"][str(aid)] = {"balance": bal, "loans": [], "id": aid}
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
    elif op == "ESTADO_PAGO_PRESTAMO":
        aid = parts[1]
        acc = data["accounts"].get(aid)
        if not acc: return "ERROR|NoExiste"
        loans = acc.get("loans", [])
        return "OK|"+json.dumps(loans)
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
