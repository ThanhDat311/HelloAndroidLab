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
        // Limit displayed length (show last part if too long)
        if (toShow.length() > 40) {
            toShow = "..." + toShow.substring(toShow.length() - 37);
        }
        textViewDisplay.setText(toShow);
    }

    private void onNumberClick(String number) {
        Log.d(TAG, "Number clicked: " + number);

        // If current display shows Error, start fresh
        if ("Error".equals(currentInput)) {
            expression.setLength(0);
            currentInput = "0";
            displayIsResultOrFirstOperand = false;
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
            if ("0".equals(currentInput)) {
                currentInput = number;
                // If expression empty or last is operator, append; else append to last token
                if (expression.length() == 0 || isOperatorChar(expression.charAt(expression.length() - 1))) {
                    expression.append(number);
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

        if ("Error".equals(currentInput)) {
            // ignore operator after error; user must clear first
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
        if (expression.length() == 0) {
            return;
        }

        try {
            double result = evaluateExpression(expression.toString());
            String formatted = formatResult(result);
            // If error (NaN/inf), show Error
            if ("Error".equals(formatted)) {
                currentInput = "Error";
                expression.setLength(0);
                updateDisplay();
                return;
            }

            // show result and prepare for next calculation
            currentInput = formatted;
            expression.setLength(0);
            expression.append(formatted); // allow chaining (press + then another number)
            displayIsResultOrFirstOperand = true;
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
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                tokens.add(s.substring(i, j));
                i = j;
                continue;
            }
            if (c == '+' || c == '*' || c == '/' ) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            if (c == '-') {
                // unary minus if at start or after operator or after '('
                if (i == 0 || isOperatorChar(s.charAt(i - 1)) || s.charAt(i - 1) == '(') {
                    // read number with leading '-'
                    int j = i + 1;
                    // allow a number immediately after unary minus
                    if (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                        while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
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
            if (c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            // unknown char - skip
            i++;
        }
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
                    return Double.NaN;
                }
            } else if (isOperatorToken(t)) {
                if (st.size() < 2) return Double.NaN;
                double b = st.pop();
                double a = st.pop();
                double res = performCalculation(a, b, t);
                st.push(res);
            }
        }
        if (st.isEmpty()) return Double.NaN;
        return st.pop();
    }

    private boolean isNumberToken(String t) {
        if (t == null) return false;
        char c0 = t.charAt(0);
        return (Character.isDigit(c0) || c0 == '-' || c0 == '.');
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
                    return Double.NaN;
                }
                return num1 / num2;
            default:
                return Double.NaN;
        }
    }

    private String formatResult(double result) {
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return "Error";
        }
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



