from flask import Flask, jsonify
import mysql.connector

app = Flask(__name__)

# Настройка подключения к MySQL
config = {
    'user': 'user',
    'password': 'qwerty123',
    'host': 'localhost',
    'database': 'project_course4',
    'raise_on_warnings': True
}

# Получение данных из таблицы products
@app.route('/products', methods=['GET'])
def get_products():
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT product_name, proteins, fats, carbs, calories FROM products")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(rows)

if __name__ == '__main__':
    app.run(debug=True)