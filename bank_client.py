#!/usr/bin/env python3

import socket, sys, time, random, threading

HOST = "localhost"
PORT = 9000


def send_and_recv(msg):
    """Envía un mensaje al servidor central y devuelve la respuesta."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, PORT))
        s.sendall(b"CLIENT_BANK|bank1\n")
        s.recv(4096)  # Mensaje de bienvenida
        s.sendall((msg + "\n").encode())
        data = s.recv(8192).decode()
        return data.strip()
    except Exception as e:
        return f"ERROR|{e}"
    finally:
        try:
            s.close()
        except:
            pass


def create_accounts(n, initial):
    """Crea 'n' cuentas con saldo inicial 'initial'."""
    resp = send_and_recv(f"CREATE_ACCOUNTS|{n}|{initial}")
    print(resp)


def consult_account(aid):
    """Consulta el estado de la cuenta con id 'aid'."""
    resp = send_and_recv(f"CONSULTAR_CUENTA|{aid}")
    print(resp)


def transfer(a, b, m):
    """Transfiere un monto exacto de una cuenta a otra."""
    msg = f"TRANSFERIR_CUENTA|{a}|{b}|{m}"
    resp = send_and_recv(msg)
    print(resp)


def create_loan(account_id, amount):
    """Crea un préstamo para una cuenta."""
    msg = f"CREAR_PRESTAMO|{account_id}|{amount}|{amount}"
    resp = send_and_recv(msg)
    print(resp)


def pay_loan(account_id, loan_id, amount):
    """Paga una parte del préstamo."""
    msg = f"PAGAR_PRESTAMO|{account_id}|{loan_id}|{amount}"
    resp = send_and_recv(msg)
    print(resp)


def loan_status(account_id):
    """Consulta el estado de préstamos de una cuenta."""
    msg = f"ESTADO_PAGO_PRESTAMO|{account_id}"
    resp = send_and_recv(msg)
    print(resp)


def arqueo():
    """Realiza un arqueo total del sistema."""
    msg = "ARQUEO"
    resp = send_and_recv(msg)
    print(resp)


def stress_test(num_tx=100, max_acc=100, max_amt=100):
    """Realiza múltiples transferencias aleatorias concurrentes (modo stress test)."""

    def worker_tx(i):
        a = random.randint(1, max_acc)
        b = random.randint(1, max_acc)
        while b == a:
            b = random.randint(1, max_acc)
        amt = round(random.random() * max_amt, 2)
        transfer(a, b, amt)
        time.sleep(random.random() * 0.1)

    threads = []
    for i in range(num_tx):
        t = threading.Thread(target=worker_tx, args=(i,))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()


if __name__ == "__main__":
    if len(sys.argv) >= 2 and sys.argv[1] == "create":
        # Por defecto crea 10000 cuentas con 100.0 cada una
        create_accounts(10000, 100.0)

    elif len(sys.argv) >= 2 and sys.argv[1] == "consult":
        if len(sys.argv) < 3:
            print("Uso: bank_client.py consult <id>")
        else:
            consult_account(sys.argv[2])

    elif len(sys.argv) >= 2 and sys.argv[1] == "transfer":
        if len(sys.argv) < 5:
            print("Uso: bank_client.py transfer <from> <to> <amt>")
        else:
            try:
                a = int(sys.argv[2])
                b = int(sys.argv[3])
                m = float(sys.argv[4])
                transfer(a, b, m)
            except ValueError:
                print("Argumentos inválidos. Uso: bank_client.py transfer <from> <to> <amt>")

    elif len(sys.argv) >= 2 and sys.argv[1] == "stress":
        num = int(sys.argv[2]) if len(sys.argv) > 2 else 200
        maxacc = int(sys.argv[3]) if len(sys.argv) > 3 else 10000
        stress_test(num, maxacc, 50)

    elif len(sys.argv) >= 2 and sys.argv[1] == "create_loan":
        if len(sys.argv) < 4:
            print("Uso: bank_client.py create_loan <account_id> <amount>")
        else:
            create_loan(sys.argv[2], float(sys.argv[3]))

    elif len(sys.argv) >= 2 and sys.argv[1] == "pay_loan":
        if len(sys.argv) < 5:
            print("Uso: bank_client.py pay_loan <account_id> <loan_id> <amount>")
        else:
            pay_loan(sys.argv[2], int(sys.argv[3]), float(sys.argv[4]))

    elif len(sys.argv) >= 2 and sys.argv[1] == "loan_status":
        if len(sys.argv) < 3:
            print("Uso: bank_client.py loan_status <account_id>")
        else:
            loan_status(sys.argv[2])

    elif len(sys.argv) >= 2 and sys.argv[1] == "arqueo":
        arqueo()

    else:
        print("Uso:")
        print("  bank_client.py create")
        print("  bank_client.py consult <id>")
        print("  bank_client.py transfer <from> <to> <amt>")
        print("  bank_client.py stress [num_tx] [max_acc]")
        print("  bank_client.py create_loan <account_id> <amount>")
        print("  bank_client.py pay_loan <account_id> <loan_id> <amount>")
        print("  bank_client.py loan_status <account_id>")
        print("  bank_client.py arqueo")
