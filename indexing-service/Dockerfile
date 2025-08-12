FROM python:3.10-slim

# 작업 디렉터리
WORKDIR /app

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 프로젝트 전체 복사 (main.py)
COPY . .

# uvicorn을 통해 api.py 안의 app 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5001"]