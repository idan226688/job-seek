![Build](https://github.com/idan226688/job-seek/actions/workflows/ci.yml/badge.svg)
# JobSeek

JobSeek is an AI-powered backend application that matches CVs to job descriptions using LLMs (e.g., LLaMA via Ollama). It is designed to help users discover the most relevant job matches based on their experience, education, and skills.

## Features

- Upload and store CVs
- Store and manage job descriptions
- Use LLMs (like llama3 via Ollama) to generate match scores
- Save match results with timestamps
- Export results as CSV for analysis

## Technologies

- Java 17
- Spring Boot
- MySQL
- Testcontainers
- Docker Compose
- Ollama (local LLM inference)
- Redis
- Qdrant

## Getting Started

### Prerequisites

- [Docker](https://www.docker.com/)
- [Ollama](https://ollama.com/) (run `ollama run llama3`)
- Java 17+
- Maven

### Setup

1. Clone the repository:

```bash
git clone https://github.com/idan226688/job-seek.git
cd job-seek
```

2. Copy the example environment file and fill in your database credentials:
   
```bash
cp .env.example .env
```

3. Start the application and MySQL using Docker Compose:

```bash
docker-compose up --build
```

This will:
- Start a MySQL 8.3 container with your credentials
- Start the Spring Boot app and connect it to MySQL

4. Make sure `ollama` is running and the model `llama3` is available:

```bash
ollama run llama3
```

## 📄 Example: Upload CV and Run Job Match

### 1. Add job descriptions

You can add job descriptions using a simple `curl` command:

```bash
curl -X POST http://localhost:8080/api/job/add \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<enter the job description here>"
}'
```

Repeat with different id values and content for multiple jobs.

### 2. Upload your CV (PDF)

Use [Postman](https://www.postman.com/) or `curl`:

A sample CV file is located at:

JobSeek/src/main/resources/my_cv.pdf

You can either upload it and trigger matching, or just send it directly for matching without uploading.

✅ Option A: Upload the CV for Reuse Later

```bash
curl -X POST http://localhost:8080/api/cv/upload \
  -F "file=@src/main/resources/my_cv.pdf" \
  -F "userId=my_cv"
```

This uploads and parses the CV, saving it under the userId.

Then run the match process:

```base
curl "http://localhost:8080/api/match/my_cv?model=llama3.2"
```

✅ Option B: Direct Match without Uploading

```bash
curl --location 'http://localhost:8080/api/match/cv' \
--header 'Content-Type: application/json' \
--data '{
    "id": "my_cv",
  "content": "<enter cv content here>"
}'
```

This sends the CV content directly and returns matches without saving the CV.

The response will return a JSON array of match results. Each result contains:

- `Id`: unique identifier of the match result
- `userId`: the CV identifier
- `jobId`: the matched job ID
- `score`: the match score (0–100)
- `timestamp`: when the score was generated


```json
[
  {
    "id": "17b71a20-bd05-46f2-8686-10b941bffe23",
    "userId": "my_cv",
    "jobId": 2,
    "score": 30,
    "timestamp": "2025-05-13T09:14:26.623940760"
  },
  {
    "id": "921e148d-c885-4a7d-a748-4e31988af23c",
    "userId": "my_cv",
    "jobId": 3,
    "score": 30,
    "timestamp": "2025-05-13T09:14:27.116304885"
  }
]
```

### 4. Fetch Results
To get the most recent matches for cv saved as "my_cv" for example:

```bash
curl "http://localhost:8080/api/match/last/my_cv"
```
Paginated view:
```bash
curl "http://localhost:8080/api/match/last/my_cv/paged?page=0&size=5"
```


### 📂 Browsing Stored Data

Once you’ve added CVs and job descriptions, you can list or retrieve them using the following endpoints:

📋 View All Job Descriptions

```bash
curl http://localhost:8080/api/job/jobs
```

🔍 View a Specific Job by ID

```bash
curl http://localhost:8080/api/job/job/<id>
```
📋 View All Uploaded CVs

```bash
curl http://localhost:8080/api/cv/cvs
```

🔍 View a Specific CV by ID
```bash
curl http://localhost:8080/api/cv/my_cv
```

