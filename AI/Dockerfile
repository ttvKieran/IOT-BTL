# Sử dụng base image Python 3.11
FROM python:3.11-slim

    # Đặt thư mục làm việc
WORKDIR /app

    # Sao chép file requirements.txt (file này rất quan trọng)
COPY requirements.txt .

    # Cài đặt các thư viện Python
RUN pip install --no-cache-dir -r requirements.txt

    # Sao chép toàn bộ code Python vào
COPY . .

    # Cổng mà FastAPI sẽ chạy bên trong container
EXPOSE 8000

    # Lệnh để chạy Uvicorn, lắng nghe trên TẤT CẢ các địa chỉ (0.0.0.0)
CMD ["uvicorn", "AIService:app", "--host", "0.0.0.0", "--port", "8000"]