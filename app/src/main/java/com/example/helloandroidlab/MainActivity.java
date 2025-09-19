package com.example.helloandroidlab;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView; // Button is implicitly used via findViewById

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView textViewDisplay;
    private String currentInput = "0";
    private Double firstOperand = null;
    private String pendingOperator = null;
    // This flag indicates if the current display value is the result of a calculation
    // or the first operand of an operation, so that the next number input replaces it.
    private boolean displayIsResultOrFirstOperand = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewDisplay = findViewById(R.id.textView);
        initializeCalculatorButtons();
        onClearClick(); // Set initial state and display "0"
    }

    private void initializeCalculatorButtons() {
        // Number buttons
        findViewById(R.id.button0).setOnClickListener(v -> onNumberClick("0"));
        findViewById(R.id.button1).setOnClickListener(v -> onNumberClick("1"));
        findViewById(R.id.button2).setOnClickListener(v -> onNumberClick("2"));
        findViewById(R.id.button3).setOnClickListener(v -> onNumberClick("3"));
        findViewById(R.id.button4).setOnClickListener(v -> onNumberClick("4"));
        findViewById(R.id.button5).setOnClickListener(v -> onNumberClick("5"));
        findViewById(R.id.button6).setOnClickListener(v -> onNumberClick("6"));
        findViewById(R.id.button7).setOnClickListener(v -> onNumberClick("7"));
        findViewById(R.id.button8).setOnClickListener(v -> onNumberClick("8"));
        findViewById(R.id.button9).setOnClickListener(v -> onNumberClick("9"));

        // Operator buttons
        findViewById(R.id.buttonAdd).setOnClickListener(v -> onOperatorClick("+"));
        findViewById(R.id.buttonSubtract).setOnClickListener(v -> onOperatorClick("-"));
        findViewById(R.id.buttonMultiply).setOnClickListener(v -> onOperatorClick("*"));
        findViewById(R.id.buttonDivide).setOnClickListener(v -> onOperatorClick("/"));

        // Equals button
        findViewById(R.id.buttonEquals).setOnClickListener(v -> onEqualsClick());

        // Clear button
        findViewById(R.id.buttonClear).setOnClickListener(v -> onClearClick());
    }

    private void updateDisplay() {
        textViewDisplay.setText(currentInput);
    }

    private void onNumberClick(String number) {
        if (currentInput.equals("Error")) {
            // If current state is Error, pressing a number starts a new calculation
            currentInput = "0";
            firstOperand = null;
            pendingOperator = null;
            displayIsResultOrFirstOperand = false;
        }

        if (displayIsResultOrFirstOperand) {
            currentInput = number;
            displayIsResultOrFirstOperand = false; // Next number will append
        } else {
            if (currentInput.equals("0")) {
                currentInput = number; // Replace "0"
            } else {
                currentInput += number; // Append
            }
        }
        updateDisplay();
    }

    private void onOperatorClick(String operator) {
        if (currentInput.equals("Error")) {
            return; // Don't do anything if already in error state
        }

        try {
            double currentNumber = Double.parseDouble(currentInput);

            if (firstOperand == null) {
                // This is the first operand
                firstOperand = currentNumber;
            } else if (pendingOperator != null) {
                // There's a pending operation (e.g., 5 + 3, then user presses '*')
                // Calculate the pending operation first
                firstOperand = performCalculation(firstOperand, currentNumber, pendingOperator);
                currentInput = formatResult(firstOperand);
                updateDisplay();

                if (currentInput.equals("Error")) {
                    // Calculation resulted in error (e.g., division by zero)
                    resetCalculatorOnError();
                    return;
                }
            }
            // Set the new pending operator
            pendingOperator = operator;
            displayIsResultOrFirstOperand = true; // Next number input will replace currentInput (which now shows firstOperand or intermediate result)
        } catch (NumberFormatException e) {
            currentInput = "Error";
            updateDisplay();
            resetCalculatorOnError();
        }
    }

    private void onEqualsClick() {
        if (currentInput.equals("Error") || firstOperand == null || pendingOperator == null) {
            // Not enough information to calculate or already in error state
            return;
        }

        try {
            double secondNumber = Double.parseDouble(currentInput);
            double result = performCalculation(firstOperand, secondNumber, pendingOperator);
            currentInput = formatResult(result);
            updateDisplay();

            // Reset for the next independent calculation
            firstOperand = null; // Or `result` if we want "Ans" functionality
            pendingOperator = null;
            displayIsResultOrFirstOperand = true; // Next number input will start a new calculation
        } catch (NumberFormatException e) {
            currentInput = "Error";
            updateDisplay();
            resetCalculatorOnError();
        }
    }

    private void onClearClick() {
        currentInput = "0";
        firstOperand = null;
        pendingOperator = null;
        displayIsResultOrFirstOperand = false;
        updateDisplay();
    }

    private void resetCalculatorOnError() {
        firstOperand = null;
        pendingOperator = null;
        displayIsResultOrFirstOperand = false; // Though currentInput is "Error", next number will start fresh due to onNumberClick logic
    }

    private double performCalculation(double num1, double num2, String op) {
        switch (op) {
            case "+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "*":
                return num1 * num2;
            case "/":
                if (num2 == 0) {
                    return Double.NaN; // Indicate division by zero
                }
                return num1 / num2;
            default:
                return Double.NaN; // Should not happen with defined operators
        }
    }

    private String formatResult(double result) {
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return "Error";
        }
        // Remove trailing ".0" for whole numbers
        if (result == (long) result) {
            return String.format("%d", (long) result);
        } else {
            // You might want to limit decimal places here for very long results
            return String.valueOf(result);
        }
    }
}

