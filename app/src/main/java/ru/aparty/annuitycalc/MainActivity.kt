package ru.aparty.annuitycalc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ru.aparty.annuitycalc.R
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var loanAmountEditText: EditText
    private lateinit var loanTermSpinner: Spinner
    private lateinit var interestRateEditText: EditText
    private lateinit var monthlyPaymentEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var modeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов
        loanAmountEditText = findViewById(R.id.loanAmountEditText)
        loanTermSpinner = findViewById(R.id.loanTermSpinner)
        interestRateEditText = findViewById(R.id.interestRateEditText)
        monthlyPaymentEditText = findViewById(R.id.monthlyPaymentEditText)
        calculateButton = findViewById(R.id.calculateButton)
        resultTextView = findViewById(R.id.resultTextView)
        modeSpinner = findViewById(R.id.modeSpinner)

        // Настройка спиннера для срока кредита
        val terms = arrayOf("6 месяцев", "12 месяцев", "24 месяца", "36 месяцев", "60 месяцев")
        val termAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        loanTermSpinner.adapter = termAdapter

        // Настройка спиннера для режима расчета
        val modes = arrayOf("Рассчитать платеж", "Рассчитать процент")
        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = modeAdapter

        // Переключение полей ввода в зависимости от режима
        modeSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateInputFields(position == 0)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        // Слушатели для автоматического пересчета при изменении полей
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateResult()
            }
        }

        loanAmountEditText.addTextChangedListener(textWatcher)
        interestRateEditText.addTextChangedListener(textWatcher)
        monthlyPaymentEditText.addTextChangedListener(textWatcher)

        // Обработчик кнопки "Рассчитать"
        calculateButton.setOnClickListener {
            calculateResult()
        }
    }

    private fun updateInputFields(isPaymentMode: Boolean) {
        interestRateEditText.isEnabled = isPaymentMode
        monthlyPaymentEditText.isEnabled = !isPaymentMode
        interestRateEditText.setText("")
        monthlyPaymentEditText.setText("")
        resultTextView.text = ""
    }

    private fun calculateResult() {
        try {
            val loanAmount = loanAmountEditText.text.toString().toDoubleOrNull() ?: return
            val termMonths = when (loanTermSpinner.selectedItemPosition) {
                0 -> 6
                1 -> 12
                2 -> 24
                3 -> 36
                4 -> 60
                else -> return
            }

            if (modeSpinner.selectedItemPosition == 0) {
                // Режим 1: Рассчитать ежемесячный платеж
                val interestRate = interestRateEditText.text.toString().toDoubleOrNull() ?: return
                val monthlyRate = interestRate / 100 / 12
                val payment = loanAmount * monthlyRate * (1 + monthlyRate).pow(termMonths) /
                        ((1 + monthlyRate).pow(termMonths) - 1)
                resultTextView.text = String.format("Ежемесячный платеж: %.2f", payment)
            } else {
                // Режим 2: Рассчитать процентную ставку
                val monthlyPayment = monthlyPaymentEditText.text.toString().toDoubleOrNull() ?: return
                val rate = calculateInterestRate(loanAmount, monthlyPayment, termMonths)
                resultTextView.text = String.format("Годовая процентная ставка: %.2f%%", rate * 100)
            }
        } catch (e: Exception) {
            resultTextView.text = "Ошибка ввода"
        }
    }

    private fun calculateInterestRate(loanAmount: Double, monthlyPayment: Double, termMonths: Int): Double {
        // Бинарный поиск для нахождения процентной ставки
        var low = 0.0
        var high = 1.0 // 100%
        var rate = 0.5
        val epsilon = 0.0001
        val maxIterations = 1000

        for (i in 0 until maxIterations) {
            val monthlyRate = rate / 12
            val calculatedPayment = loanAmount * monthlyRate * (1 + monthlyRate).pow(termMonths) /
                    ((1 + monthlyRate).pow(termMonths) - 1)

            if (Math.abs(calculatedPayment - monthlyPayment) < epsilon) {
                return rate
            }

            if (calculatedPayment > monthlyPayment) {
                high = rate
            } else {
                low = rate
            }
            rate = (low + high) / 2
        }
        return rate
    }
}