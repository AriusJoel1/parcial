#!/bin/bash
# Script to run 3 workers in background
python worker_node.py w0 &> worker_w0.log &
python worker_node.py w1 &> worker_w1.log &
python worker_node.py w2 &> worker_w2.log &
echo "Workers started (w0,w1,w2). Logs: worker_w*.log"
