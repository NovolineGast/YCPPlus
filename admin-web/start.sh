#!/bin/bash

echo "================================================"
echo "YCPPlus Admin Web - Quick Start"
echo "================================================"
echo ""

cd "$(dirname "$0")"

echo "[1/3] Starting Backend..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

sleep 5

echo "[2/3] Installing Frontend Dependencies..."
cd frontend
if [ ! -d "node_modules" ]; then
    npm install
fi

echo "[3/3] Starting Frontend..."
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "================================================"
echo "Services Started"
echo "================================================"
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "Open your browser and visit http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop all services"
echo "================================================"

trap "kill $BACKEND_PID $FRONTEND_PID; exit" INT TERM

wait
