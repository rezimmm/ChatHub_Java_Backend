@echo off
SET MVN="C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
SET JWT_SECRET_KEY=CgUu5hMWeF/LUpup1SIwFd1TBt8FRP6ScUcSeO9b/z3RmmkI7b2DBjWyocdpd9nE2tOTeKh2JYPq+1lHusZUVg==
SET MONGO_URL=mongodb+srv://rezim:rajput2157@cluster0.zzdnx4o.mongodb.net/chathub
SET DB_NAME=chathub

echo Compiling ChatHub Java Backend...
%MVN% clean compile -q
IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo COMPILATION FAILED - check errors above
    exit /b 1
)
echo Compilation successful!
