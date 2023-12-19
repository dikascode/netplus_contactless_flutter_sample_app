
import 'package:flutter/services.dart';

class PlatformService {
  // Define a MethodChannel with a unique name to establish communication between Flutter and native
  // platform code for the 'netplus/contactless_sdk' functionalities.
  static const platform = MethodChannel('netplus/contactless_sdk');

  static Future<String?> startPayment(double amount) async {
    try {
      final String result = await platform.invokeMethod('startPayment', {'amount': amount});
      return result;
    } on PlatformException catch (e) {
      throw PlatformException(code: e.code, message: e.message);
    }
  }

  static Future<String?> checkBalance() async {
    try {
      final String result = await platform.invokeMethod('checkBalance');
      return result;
    } on PlatformException catch (e) {
      throw PlatformException(code: e.code, message: e.message);
    }
  }
}