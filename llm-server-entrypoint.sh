#!/bin/bash

# Default model to pull if OLLAMA_MODEL_TO_PULL is not set
DEFAULT_MODEL="gpt-oss:20b"
MODEL_TO_PULL=${OLLAMA_MODEL_TO_PULL:-$DEFAULT_MODEL}

echo "Starting Ollama server..."
ollama serve &
pid=$!

# Wait for Ollama server to be active using curl
echo "Waiting for Ollama server to be active..."
until curl -s http://localhost:11434/api/tags > /dev/null; do
  sleep 1
done
echo
echo "Ollama server is active."

echo "Pulling the LLM model (${MODEL_TO_PULL})..."
ollama pull "${MODEL_TO_PULL}"
echo "Model pull complete."

wait $pid