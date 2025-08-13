#!/bin/bash

echo "Starting Ollama server..."
ollama serve &
pid=$!

echo "Waiting for Ollama server to be active..."
while [ "$(ollama list | grep 'NAME')" == "" ]; do
  sleep 1
done
echo
echo "Ollama server is active."

echo "Pulling the LLM model (llama3:8b)..."
ollama pull gpt-oss:20b
echo "Model pull complete."

wait $pid