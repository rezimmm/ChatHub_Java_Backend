@echo off
REM ChatHub Java Backend Startup Script
REM This script starts the Java Spring Boot backend with the correct environment variables.

SET JWT_SECRET_KEY=CgUu5hMWeF/LUpup1SIwFd1TBt8FRP6ScUcSeO9b/z3RmmkI7b2DBjWyocdpd9nE2tOTeKh2JYPq+1lHusZUVg==
SET MONGO_URL=mongodb+srv://rezim:rajput2157@cluster0.zzdnx4o.mongodb.net/chathub
SET DB_NAME=chathub

echo Starting ChatHub Java Backend...
echo MongoDB: [configured]
echo Port: 8000

"C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" clean spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m"
