#!/usr/bin/env pwsh

# Simple test script for auth flow
$baseUrl = "http://localhost:8080"
$uniqueId = [DateTime]::Now.Ticks.ToString().Substring(0, 5)

# Test 1: Register
Write-Host "=== Test 1: Registration ===" -ForegroundColor Green
$registrationBody = @{
    username = "testuser_$uniqueId"
    email = "test_$uniqueId@example.com"
    password = "Test@1234567"
    firstName = "TestFirst"
    lastName = "TestLast"
} | ConvertTo-Json -Compress

$registrationResponse = $null
try {
    $registrationResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registrationBody `
        -UseBasicParsing
    
    Write-Host "[PASS] Registration successful: $($registrationResponse.StatusCode)" -ForegroundColor Green
    $regData = $registrationResponse.Content | ConvertFrom-Json
    $username = $regData.data.user.username
    $accessToken = $regData.data.accessToken
    $refreshToken = $regData.data.refreshToken
    
    Write-Host "  - Username: $username"
    Write-Host "  - AccessToken: $($accessToken.Substring(0, 20))..."
    
} catch {
    Write-Host "[FAIL] Registration failed: $_" -ForegroundColor Red
    exit 1
}

# Test 2: Login
Write-Host "`n=== Test 2: Login ===" -ForegroundColor Green
$loginBody = @{
    usernameOrEmail = $username
    password = "Test@1234567"
} | ConvertTo-Json -Compress

$loginResponse = $null
try {
    $loginResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody `
        -UseBasicParsing
    
    Write-Host "[PASS] Login successful: $($loginResponse.StatusCode)" -ForegroundColor Green
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "  - LoginToken: $($loginData.data.accessToken.Substring(0, 20))..."
    
} catch {
    Write-Host "[FAIL] Login failed: $_" -ForegroundColor Red
    exit 1
}

# Test 3: Get current user
Write-Host "`n=== Test 3: Get Current User ===" -ForegroundColor Green
try {
    $meResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $accessToken"} `
        -UseBasicParsing
    
    Write-Host "[PASS] Get user successful: $($meResponse.StatusCode)" -ForegroundColor Green
    $meData = $meResponse.Content | ConvertFrom-Json
    Write-Host "  - User: $($meData.data.username)"
    Write-Host "  - Email: $($meData.data.email)"
    
} catch {
    Write-Host "[FAIL] Get user failed: $_" -ForegroundColor Red
    exit 1
}

# Test 4: Refresh token
Write-Host "`n=== Test 4: Refresh Token ===" -ForegroundColor Green
try {
    $refreshResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/refresh" `
        -Method Post `
        -Headers @{"Refresh-Token" = $refreshToken} `
        -UseBasicParsing
    
    Write-Host "[PASS] Refresh successful: $($refreshResponse.StatusCode)" -ForegroundColor Green
    $refreshData = $refreshResponse.Content | ConvertFrom-Json
    Write-Host "  - NewAccessToken: $($refreshData.data.accessToken.Substring(0, 20))..."
    
} catch {
    Write-Host "[FAIL] Refresh failed: $_" -ForegroundColor Red
    exit 1
}

# Test 5: Logout
Write-Host "`n=== Test 5: Logout ===" -ForegroundColor Green
try {
    $logoutResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/logout" `
        -Method Post `
        -Headers @{"Authorization" = "Bearer $accessToken"} `
        -UseBasicParsing
    
    Write-Host "[PASS] Logout successful: $($logoutResponse.StatusCode)" -ForegroundColor Green
    
} catch {
    Write-Host "[FAIL] Logout failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== All tests passed! ===" -ForegroundColor Green
