@echo off
REM Delete the 8 OTP-related files
del "src\main\java\com\smartloan\service\OtpService.java"
del "src\main\java\com\smartloan\entity\Otp.java"
del "src\main\java\com\smartloan\entity\OtpType.java"
del "src\main\java\com\smartloan\repository\OtpRepository.java"
del "src\main\java\com\smartloan\dto\SendEmailOtpRequest.java"
del "src\main\java\com\smartloan\dto\SendSmsOtpRequest.java"
del "src\main\java\com\smartloan\dto\VerifyOtpRequest.java"
del "src\main\java\com\smartloan\dto\OtpResponse.java"

echo All 8 OTP files deleted successfully
