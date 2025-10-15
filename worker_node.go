package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	CENTRAL_HOST = "localhost"
	CENTRAL_PORT = 9000
)

type Loan struct {
	ID              int     `json:"id"`
	IDCliente       int     `json:"id_cliente"`
	Monto           float64 `json:"monto"`
	MontoPendiente  float64 `json:"monto_pendiente"`
	Estado          string  `json:"estado"`
	FechaSolicitud  string  `json:"fecha_solicitud"`
}

type Account struct {
	Balance   float64 `json:"balance"`
	Loans     []int   `json:"loans"`
	ID        int     `json:"id"`
	IDCliente int     `json:"id_cliente"`
}

type Transaction struct {
	Type   string  `json:"type"`
	Amount float64 `json:"amount"`
	To     string  `json:"to,omitempty"`
	From   string  `json:"from,omitempty"`
}

type Storage struct {
	Accounts     map[string]Account            `json:"accounts"`
	Transactions map[string][]Transaction      `json:"transactions"`
	Loans        map[string][]Loan             `json:"loans"`
	LoanCounter  int                           `json:"loan_counter"`
	mu           sync.Mutex
}

var storage Storage
var storageFile string
var workerID string

func main() {
	workerID = "w_go1"
	if len(os.Args) > 1 {
		workerID = os.Args[1]
	}

	storageFile = fmt.Sprintf("worker_%s_data.json", workerID)
	loadStorage()

	for {
		conn, err := net.Dial("tcp", fmt.Sprintf("%s:%d", CENTRAL_HOST, CENTRAL_PORT))
		if err != nil {
			fmt.Printf("Worker %s: no puedo conectar al servidor (%v). Reintentando en 2s...\n", workerID, err)
			time.Sleep(2 * time.Second)
			continue
		}

		fmt.Fprintf(conn, "WORKER|%s\n", workerID)
		fmt.Printf("Worker %s conectado al servidor %s:%d\n", workerID, CENTRAL_HOST, CENTRAL_PORT)

		scanner := bufio.NewScanner(conn)
		for scanner.Scan() {
			cmd := scanner.Text()
			if cmd == "" {
				continue
			}
			response := handleCommand(cmd)
			fmt.Fprintf(conn, "%s\n", response)
		}

		conn.Close()
		fmt.Printf("Worker %s desconectado normalmente, saliendo.\n", workerID)
		break
	}
}

func handleCommand(cmd string) string {
	parts := strings.Split(cmd, "|")
	op := parts[0]

	switch op {
	case "CREATE_ACCOUNT":
		return createAccount(parts)
	case "CONSULTAR_CUENTA":
		return consultAccount(parts)
	case "DEBIT":
		return debit(parts)
	case "CREDIT":
		return credit(parts)
	case "RECORD_TX":
		return recordTx(parts)
	case "CREAR_PRESTAMO":
		return createLoan(parts)
	case "PAGAR_PRESTAMO":
		return payLoan(parts)
	case "ESTADO_PAGO_PRESTAMO":
		return loanStatus(parts)
	case "ARQUEO":
		return arqueo()
	default:
		return "ERROR|UnknownOp"
	}
}

func createAccount(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	id := parts[1]
	balance, _ := strconv.ParseFloat(parts[2], 64)

	idNum, _ := strconv.Atoi(id)
	storage.Accounts[id] = Account{
		Balance:   balance,
		Loans:     []int{},
		ID:        idNum,
		IDCliente: idNum,
	}
	saveStorage()
	return "OK"
}

func consultAccount(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	id := parts[1]
	acc, exists := storage.Accounts[id]
	if !exists {
		return "ERROR|NoExiste"
	}

	data, _ := json.Marshal(acc)
	return "OK|" + string(data)
}

func debit(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	id := parts[1]
	amount, _ := strconv.ParseFloat(parts[2], 64)

	acc, exists := storage.Accounts[id]
	if !exists {
		return "ERROR|NoExiste"
	}
	if acc.Balance < amount {
		return "ERROR|SaldoInsuficiente"
	}

	acc.Balance -= amount
	storage.Accounts[id] = acc

	storage.Transactions[id] = append(storage.Transactions[id], Transaction{
		Type:   "debit",
		Amount: amount,
	})

	saveStorage()
	return "OK"
}

func credit(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	id := parts[1]
	amount, _ := strconv.ParseFloat(parts[2], 64)

	acc, exists := storage.Accounts[id]
	if !exists {
		return "ERROR|NoExiste"
	}

	acc.Balance += amount
	storage.Accounts[id] = acc

	storage.Transactions[id] = append(storage.Transactions[id], Transaction{
		Type:   "credit",
		Amount: amount,
	})

	saveStorage()
	return "OK"
}

func recordTx(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	src := parts[1]
	dst := parts[2]
	amount, _ := strconv.ParseFloat(parts[3], 64)

	storage.Transactions[src] = append(storage.Transactions[src], Transaction{
		Type:   "transfer_out",
		To:     dst,
		Amount: amount,
	})

	storage.Transactions[dst] = append(storage.Transactions[dst], Transaction{
		Type:   "transfer_in",
		From:   src,
		Amount: amount,
	})

	saveStorage()
	return "OK"
}

func createLoan(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	aid := parts[1]
	amount, _ := strconv.ParseFloat(parts[2], 64)
	pending, _ := strconv.ParseFloat(parts[3], 64)

	acc, exists := storage.Accounts[aid]
	if !exists {
		return "ERROR|NoExiste"
	}

	storage.LoanCounter++
	loanID := storage.LoanCounter

	loan := Loan{
		ID:             loanID,
		IDCliente:      acc.IDCliente,
		Monto:          amount,
		MontoPendiente: pending,
		Estado:         "Activo",
		FechaSolicitud: time.Now().Format("2006-01-02 15:04:05"),
	}

	storage.Loans[aid] = append(storage.Loans[aid], loan)
	acc.Loans = append(acc.Loans, loanID)
	storage.Accounts[aid] = acc

	saveStorage()
	return fmt.Sprintf("OK|LoanID:%d", loanID)
}

func payLoan(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	aid := parts[1]
	loanID, _ := strconv.Atoi(parts[2])
	amount, _ := strconv.ParseFloat(parts[3], 64)

	_, exists := storage.Accounts[aid]
	if !exists {
		return "ERROR|NoExiste"
	}

	loans := storage.Loans[aid]
	loanFound := false
	for i := range loans {
		if loans[i].ID == loanID {
			loanFound = true
			if loans[i].MontoPendiente < amount {
				return "ERROR|MontoExcedeMontoPendiente"
			}

			loans[i].MontoPendiente -= amount
			if loans[i].MontoPendiente <= 0 {
				loans[i].Estado = "Cancelado"
				loans[i].MontoPendiente = 0
			}

			storage.Loans[aid] = loans
			saveStorage()
			return fmt.Sprintf("OK|MontoRestante:%.2f", loans[i].MontoPendiente)
		}
	}

	if !loanFound {
		return "ERROR|PrestamoNoExiste"
	}

	return "ERROR|PrestamoNoExiste"
}

func loanStatus(parts []string) string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	aid := parts[1]
	_, exists := storage.Accounts[aid]
	if !exists {
		return "ERROR|NoExiste"
	}

	loans := storage.Loans[aid]
	loanDetails := []map[string]interface{}{}

	for _, loan := range loans {
		montoPagado := loan.Monto - loan.MontoPendiente
		loanDetails = append(loanDetails, map[string]interface{}{
			"id_prestamo":      loan.ID,
			"monto_total":      loan.Monto,
			"monto_pagado":     montoPagado,
			"monto_pendiente":  loan.MontoPendiente,
			"estado":           loan.Estado,
		})
	}

	data, _ := json.Marshal(loanDetails)
	return "OK|" + string(data)
}

func arqueo() string {
	storage.mu.Lock()
	defer storage.mu.Unlock()

	totalBalance := 0.0
	totalAccounts := len(storage.Accounts)

	for _, acc := range storage.Accounts {
		totalBalance += acc.Balance
	}

	return fmt.Sprintf("OK|%.2f|%d", totalBalance, totalAccounts)
}

func loadStorage() {
	storage.Accounts = make(map[string]Account)
	storage.Transactions = make(map[string][]Transaction)
	storage.Loans = make(map[string][]Loan)
	storage.LoanCounter = 0

	data, err := os.ReadFile(storageFile)
	if err != nil {
		saveStorage()
		return
	}

	json.Unmarshal(data, &storage)
}

func saveStorage() {
	data, _ := json.MarshalIndent(storage, "", "  ")
	os.WriteFile(storageFile, data, 0644)
}
