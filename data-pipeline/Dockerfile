FROM python:3.12.12-slim
WORKDIR /pipeline
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY pipeline.py importer.py ./
ENTRYPOINT ["python", "-u", "pipeline.py"]
CMD ["--help"]
