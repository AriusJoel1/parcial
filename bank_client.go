package main

import (
	"bufio"
	"fmt"
	"math/rand"
	"net"
	"os"
	"strconv"
	"sync"
	"time"
)

const (
	HOST = "localhost"
	PORT = 9000
)

func sendAndRecv(msg string) string {
	conn, err := net.Dial("tcp", fmt.Sprintf("%s:%d", HOST, PORT))
	if err != nil {
		return fmt.Sprintf("ERROR|%v", err)
	}
	defer conn.Close()

	// Identificarse como cliente bancario
	fmt.Fprintf(conn, "CLIENT_BANK|bank_go\n")

	// Leer mensaje de bienvenida
	scanner := bufio.NewScanner(conn)
	scanner.Scan()

	// Enviar comando
	fmt.Fprintf(conn, "%s\n", msg)

	// Leer respuesta
	if scanner.Scan() {
		return scanner.Text()
	}

	return "ERROR|NoResponse"
}

func createAccounts(n int, initial float64) {
	msg := fmt.Sprintf("CREATE_ACCOUNTS|%d|%.2f", n, initial)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func consultAccount(aid int) {
	msg := fmt.Sprintf("CONSULTAR_CUENTA|%d", aid)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func transfer(from, to int, amount float64) {
	msg := fmt.Sprintf("TRANSFERIR_CUENTA|%d|%d|%.2f", from, to, amount)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func createLoan(accountID int, amount float64) {
	msg := fmt.Sprintf("CREAR_PRESTAMO|%d|%.2f|%.2f", accountID, amount, amount)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func payLoan(accountID, loanID int, amount float64) {
	msg := fmt.Sprintf("PAGAR_PRESTAMO|%d|%d|%.2f", accountID, loanID, amount)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func loanStatus(accountID int) {
	msg := fmt.Sprintf("ESTADO_PAGO_PRESTAMO|%d", accountID)
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func arqueo() {
	msg := "ARQUEO"
	resp := sendAndRecv(msg)
	fmt.Println(resp)
}

func stressTest(numTx, maxAcc int, maxAmt float64) {
	fmt.Printf("Iniciando stress test: %d transacciones con cuentas 1-%d\n", numTx, maxAcc)

	var wg sync.WaitGroup
	rand.Seed(time.Now().UnixNano())

	for i := 0; i < numTx; i++ {
		wg.Add(1)
		go func(txNum int) {
			defer wg.Done()

			from := rand.Intn(maxAcc) + 1
			to := rand.Intn(maxAcc) + 1
			for to == from {
				to = rand.Intn(maxAcc) + 1
			}

			amount := rand.Float64() * maxAmt
			transfer(from, to, amount)

			// Delay aleatorio
			time.Sleep(time.Duration(rand.Intn(100)) * time.Millisecond)
		}(i)
	}

	wg.Wait()
	fmt.Println("Stress test completado")
}

func main() {
	if len(os.Args) < 2 {
		printUsage()
		return
	}

	cmd := os.Args[1]

	switch cmd {
	case "create":
		createAccounts(10000, 100.0)

	case "consult":
		if len(os.Args) < 3 {
			fmt.Println("Uso: bank_client consult <id>")
			return
		}
		id, _ := strconv.Atoi(os.Args[2])
		consultAccount(id)

	case "transfer":
		if len(os.Args) < 5 {
			fmt.Println("Uso: bank_client transfer <from> <to> <amount>")
			return
		}
		from, _ := strconv.Atoi(os.Args[2])
		to, _ := strconv.Atoi(os.Args[3])
		amount, _ := strconv.ParseFloat(os.Args[4], 64)
		transfer(from, to, amount)

	case "stress":
		numTx := 200
		maxAcc := 10000
		if len(os.Args) > 2 {
			numTx, _ = strconv.Atoi(os.Args[2])
		}
		if len(os.Args) > 3 {
			maxAcc, _ = strconv.Atoi(os.Args[3])
		}
		stressTest(numTx, maxAcc, 50.0)

	case "create_loan":
		if len(os.Args) < 4 {
			fmt.Println("Uso: bank_client create_loan <account_id> <amount>")
			return
		}
		accountID, _ := strconv.Atoi(os.Args[2])
		amount, _ := strconv.ParseFloat(os.Args[3], 64)
		createLoan(accountID, amount)

	case "pay_loan":
		if len(os.Args) < 5 {
			fmt.Println("Uso: bank_client pay_loan <account_id> <loan_id> <amount>")
			return
		}
		accountID, _ := strconv.Atoi(os.Args[2])
		loanID, _ := strconv.Atoi(os.Args[3])
		amount, _ := strconv.ParseFloat(os.Args[4], 64)
		payLoan(accountID, loanID, amount)

	case "loan_status":
		if len(os.Args) < 3 {
			fmt.Println("Uso: bank_client loan_status <account_id>")
			return
		}
		accountID, _ := strconv.Atoi(os.Args[2])
		loanStatus(accountID)

	case "arqueo":
		arqueo()

	default:
		printUsage()
	}
}

func printUsage() {
	fmt.Println("Uso:")
	fmt.Println("  bank_client create")
	fmt.Println("  bank_client consult <id>")
	fmt.Println("  bank_client transfer <from> <to> <amount>")
	fmt.Println("  bank_client stress [num_tx] [max_acc]")
	fmt.Println("  bank_client create_loan <account_id> <amount>")
	fmt.Println("  bank_client pay_loan <account_id> <loan_id> <amount>")
	fmt.Println("  bank_client loan_status <account_id>")
	fmt.Println("  bank_client arqueo")
}
