import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:logger/logger.dart';
import 'package:netpos_contactless_flutter_sample/platform_service.dart';

void main() {
  runApp(const MyApp());
}

var logger = Logger(
  printer: PrettyPrinter(),
);

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NETPLUS Contactless Flutter Sample App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: const MyHomePage(title: 'NETPLUS Contactless Flutter Sample App'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final TextEditingController _amountController = TextEditingController();
  String _response = 'Your Response will appear here';

  @override
  void initState() {
    super.initState();
    PlatformService.platform.setMethodCallHandler(_handleMethodCall);
  }

  // This will keep track of the current loader dialog to dismiss it later
  bool _isLoaderShowing = false;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'showLoader':
        if (!_isLoaderShowing) {
          showDialog(
            context: context,
            barrierDismissible: false,
            builder: (BuildContext context) {
              _isLoaderShowing = true;
              return const Center(child: CircularProgressIndicator());
            },
          );
        }
        break;
      case 'dismissLoader':
        if (_isLoaderShowing) {
          Navigator.of(context).pop(); // Dismiss the loader dialog
          _isLoaderShowing = false;
        }
        break;
      case 'updateStatus':
      case 'updatePaymentResult':
        setState(() {
          _response = call.arguments['message'];
        });
        break;
      case 'showError':
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(call.arguments['message']),
          ),
        );
        break;
      default:
        logger.d('Unknown method called from native code: ${call.method}');
    }
  }

  void _makePayment() async {
    final amount = double.tryParse(_amountController.text);
    _amountController.clear(); //Clear input
    if (amount != null && amount > 0) {
      _response = "";
      try {
        final result = await PlatformService.startPayment(amount);
        setState(() {
          _response = result ?? 'Payment successful';
        });
      } on PlatformException catch (e) {
        setState(() {
          _response = 'Failed to make payment: ${e.message}';
        });
      }
    } else {
      setState(() {
        _response = 'Please enter a valid amount';
      });
    }
  }

  void _checkBalance() async {
    try {
      final result = await PlatformService.checkBalance();
      setState(() {
        _response = result ?? 'Balance check successful';
      });
    } on PlatformException catch (e) {
      setState(() {
        _response = 'Failed to check balance: ${e.message}';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            TextField(
              controller: _amountController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Enter Amount',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _makePayment,
              child: const Text('MAKE PAYMENT'),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _checkBalance,
              style: ElevatedButton.styleFrom(
                  primary: Theme.of(context).colorScheme.primary,
                  onPrimary: Colors.white),
              child: const Text('CHECK BALANCE'),
            ),
            const SizedBox(height: 24),
            Text(
              _response,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 16.0,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
