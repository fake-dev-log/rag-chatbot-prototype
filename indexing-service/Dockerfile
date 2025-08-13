FROM python:3.12-slim

# 작업 디렉터리
WORKDIR /app

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5001"]