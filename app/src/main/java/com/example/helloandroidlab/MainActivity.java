package com.example.helloandroidlab;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MAX_EXPRESSION_LENGTH = 1000;

    private TextView textViewDisplay;
    // expression holds the whole infix expression as user types (e.g. "12+3*4")
    private StringBuilder expression = new StringBuilder();
    // currentInput is the currently typed number token (for convenience)
    private String currentInput = "0";
    // flag: if true, next number input should replace the displayed value (start new number)
    private boolean displayIsResultOrFirstOperand = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge: use WindowCompat (more standard than a nonstandard EdgeToEdge class)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // apply system bar insets to root view padding (keeps layout visible under system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewDisplay = findViewById(R.id.textView);
        initializeCalculatorButtons();
        onClearClick(); // initial state
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
        // Show full expression when available, otherwise show currentInput
        String toShow = expression.length() > 0 ? expression.toString() : currentInput;
        // Limit displayed length (show first part if too long)
        if (toShow.length() > 40) {
            toShow = toShow.substring(0, 37) + "...";
        }
        textViewDisplay.setText(toShow);
    }

    private void onNumberClick(String number) {
        Log.d(TAG, "Number clicked: " + number);

        // If current display shows Error, start fresh
        if (currentInput.startsWith("Error")) {
            expression.setLength(0);
            currentInput = "0";
            displayIsResultOrFirstOperand = false;
        }

        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            currentInput = "Error: Expression too long";
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
            return;
        }

        if (displayIsResultOrFirstOperand) {
            // start new token (replace)
            currentInput = number;
            // If previous expression ended with operator, append new number, else start fresh expression
            if (expression.length() == 0 || isOperatorChar(expression.charAt(expression.length() - 1))) {
                // append number to expression
                expression.append(number);
            } else {
                // previous expression was a result (we replaced), clear and start new expression
                expression.setLength(0);
                expression.append(number);
            }
            displayIsResultOrFirstOperand = false;
        } else {
            // append digits to currentInput and expression properly
            if ("0".equals(currentInput) && !number.equals("0")) {
                currentInput = number;
                // Replace the last '0' in expression if it's a standalone zero
                if (expression.length() > 0 && expression.charAt(expression.length() - 1) == '0' &&
                        (expression.length() == 1 || isOperatorChar(expression.charAt(expression.length() - 2)))) {
                    expression.setCharAt(expression.length() - 1, number.charAt(0));
                } else {
                    expression.append(number);
                }
            } else {
                currentInput += number;
                expression.append(number);
            }
        }
        updateDisplay();
    }

    private void onOperatorClick(String operator) {
        Log.d(TAG, "Operator clicked: " + operator);

        if (currentInput.startsWith("Error")) {
            // ignore operator after error; user must clear first
            return;
        }

        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            currentInput = "Error: Expression too long";
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
            return;
        }

        if (expression.length() == 0) {
            // allow unary minus (start negative number)
            if (operator.equals("-")) {
                expression.append("-");
                currentInput = "-";
                displayIsResultOrFirstOperand = false;
                updateDisplay();
            }
            // otherwise ignore leading + * /
            return;
        }

        // If last char is an operator, replace it (so user can change operator easily)
        char last = expression.charAt(expression.length() - 1);
        if (isOperatorChar(last)) {
            expression.setCharAt(expression.length() - 1, operator.charAt(0));
        } else {
            expression.append(operator);
        }

        displayIsResultOrFirstOperand = true; // next number will replace currentInput
        updateDisplay();
    }

    private void onEqualsClick() {
        Log.d(TAG, "Equals clicked. Expression = " + expression.toString());
        if (expression.length() == 0 || isOperatorChar(expression.charAt(expression.length() - 1))) {
            // Empty or ends with operator -> invalid
            currentInput = "Error: Invalid expression";
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
            return;
        }

        try {
            double result = evaluateExpression(expression.toString());
            String formatted = formatResult(result);
            currentInput = formatted;
            expression.setLength(0);
            expression.append(formatted); // allow chaining (press + then another number)
            displayIsResultOrFirstOperand = true;
            updateDisplay();
        } catch (ArithmeticException e) {
            Log.e(TAG, "Evaluation error", e);
            currentInput = "Error: " + e.getMessage();
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Evaluation error", e);
            currentInput = "Error: " + e.getMessage();
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Evaluation error", e);
            currentInput = "Error";
            expression.setLength(0);
            displayIsResultOrFirstOperand = false;
            updateDisplay();
        }
    }

    private void onClearClick() {
        currentInput = "0";
        expression.setLength(0);
        displayIsResultOrFirstOperand = false;
        updateDisplay();
    }

    // ---------- Expression evaluation (shunting-yard -> RPN -> eval) ----------
    // Supported tokens: numbers (with optional decimal), unary minus (handled during tokenization),
    // operators + - * /, parentheses (if you later add buttons).
    private double evaluateExpression(String expr) {
        List<String> tokens = tokenize(expr);
        List<String> rpn = infixToRPN(tokens);
        return evaluateRPN(rpn);
    }

    private List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int openParenCount = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                int dotCount = 0;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                    if (s.charAt(j) == '.') dotCount++;
                    if (dotCount > 1) throw new IllegalArgumentException("Multiple decimal points in number");
                    j++;
                }
                tokens.add(s.substring(i, j));
                i = j;
                continue;
            }
            if (c == '+' || c == '*' || c == '/' ) {
                // Check for consecutive operators
                if (i > 0 && isOperatorChar(s.charAt(i - 1))) {
                    throw new IllegalArgumentException("Invalid consecutive operators");
                }
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            if (c == '-') {
                // Check for consecutive operators
                if (i > 0 && isOperatorChar(s.charAt(i - 1)) && s.charAt(i - 1) != '(') {
                    throw new IllegalArgumentException("Invalid consecutive operators");
                }
                // unary minus if at start or after operator or after '('
                if (i == 0 || isOperatorChar(s.charAt(i - 1)) || s.charAt(i - 1) == '(') {
                    // read number with leading '-'
                    int j = i + 1;
                    int dotCount = 0;
                    // allow a number immediately after unary minus
                    if (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                        j++; // skip the first digit or dot
                        while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                            if (s.charAt(j) == '.') dotCount++;
                            if (dotCount > 1) throw new IllegalArgumentException("Multiple decimal points in number");
                            j++;
                        }
                        tokens.add(s.substring(i, j)); // include '-'
                        i = j;
                        continue;
                    } else {
                        // treat as negative zero fallback (rare since UI appends digits)
                        tokens.add("-0");
                        i++;
                        continue;
                    }
                } else {
                    tokens.add("-");
                    i++;
                    continue;
                }
            }
            if (c == '(') {
                openParenCount++;
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            if (c == ')') {
                openParenCount--;
                if (openParenCount < 0) throw new IllegalArgumentException("Mismatched parentheses");
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            // unknown char - skip
            i++;
        }
        if (openParenCount != 0) throw new IllegalArgumentException("Mismatched parentheses");
        return tokens;
    }

    private List<String> infixToRPN(List<String> tokens) {
        List<String> out = new ArrayList<>();
        Stack<String> ops = new Stack<>();
        for (String t : tokens) {
            if (isNumberToken(t)) {
                out.add(t);
            } else if (isOperatorToken(t)) {
                while (!ops.isEmpty() && isOperatorToken(ops.peek())
                        && precedence(ops.peek()) >= precedence(t)) {
                    out.add(ops.pop());
                }
                ops.push(t);
            } else if ("(".equals(t)) {
                ops.push(t);
            } else if (")".equals(t)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                    out.add(ops.pop());
                }
                if (!ops.isEmpty() && "(".equals(ops.peek())) {
                    ops.pop();
                }
            }
        }
        while (!ops.isEmpty()) {
            out.add(ops.pop());
        }
        return out;
    }

    private double evaluateRPN(List<String> rpn) {
        Stack<Double> st = new Stack<>();
        for (String t : rpn) {
            if (isNumberToken(t)) {
                try {
                    st.push(Double.parseDouble(t));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format: " + t);
                }
            } else if (isOperatorToken(t)) {
                if (st.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: insufficient operands for operator " + t);
                }
                double b = st.pop();
                double a = st.pop();
                double res = performCalculation(a, b, t);
                st.push(res);
            }
        }
        if (st.isEmpty()) throw new IllegalArgumentException("Invalid expression: no result");
        if (st.size() > 1) throw new IllegalArgumentException("Invalid expression: extra operands");
        return st.pop();
    }

    private boolean isNumberToken(String t) {
        if (t == null || t.isEmpty()) return false;
        char c0 = t.charAt(0);
        if (c0 == '-') {
            // Exclude single '-' (operator), but allow negative numbers like '-3' or '-0'
            if (t.length() == 1) return false;
            // Assume the rest is valid (digits or decimal)
            return true;
        }
        return Character.isDigit(c0) || c0 == '.';
    }

    private boolean isOperatorToken(String t) {
        return "+".equals(t) || "-".equals(t) || "*".equals(t) || "/".equals(t);
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private int precedence(String op) {
        if ("*".equals(op) || "/".equals(op)) return 2;
        if ("+".equals(op) || "-".equals(op)) return 1;
        return 0;
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
                    throw new ArithmeticException("Division by zero");
                }
                return num1 / num2;
            default:
                throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    private String formatResult(double result) {
        // If it's a whole number, show without decimal
        if (Math.abs(result - Math.rint(result)) < 1e-12) {
            return String.format("%d", (long) Math.rint(result));
        } else {
            // limit decimal places, remove trailing zeros
            DecimalFormat df = new DecimalFormat("#.##########"); // up to 10 fractional digits
            df.setGroupingUsed(false);
            return df.format(result);
        }
    }
}