import sys
import json
import os
import numpy as np
import joblib
import tensorflow as tf

# === Get absolute path to models directory relative to this file ===
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(BASE_DIR, 'models')

# === Load models ===
xgb = joblib.load(os.path.join(MODEL_DIR, 'xgboost_model.pkl'))
rf = joblib.load(os.path.join(MODEL_DIR, 'random_forest_model.pkl'))
cnn = tf.keras.models.load_model(os.path.join(MODEL_DIR, 'cnn_model.h5'))
scaler_rf = joblib.load(os.path.join(MODEL_DIR, 'scaler_rf.pkl'))
scaler = joblib.load(os.path.join(MODEL_DIR, 'scaler.pkl'))
pca = joblib.load(os.path.join(MODEL_DIR, 'pca_model.pkl'))

# === One-hot encoding setup (same order as training) ===
type_categories = ['CASH_OUT', 'TRANSFER', 'PAYMENT', 'CASH_IN', 'DEBIT']
pair_categories = ['cc', 'cm']
part_day_categories = ['morning', 'afternoon', 'evening', 'night']

def one_hot_encode(value, categories):
    return [1 if value == cat else 0 for cat in categories]

# === Read JSON input from stdin ===
input_json = sys.stdin.read()
data = json.loads(input_json)

# === Compose feature vector ===
type_encoded = one_hot_encode(data["type"], type_categories)
pair_encoded = one_hot_encode(data["transaction_pair_code"], pair_categories)
part_encoded = one_hot_encode(data["part_of_the_day"], part_day_categories)

features = np.array([
    data["amount"],
    data["day"],
    *type_encoded,
    *pair_encoded,
    *part_encoded
]).reshape(1, -1)

# === Scale for RF ===
scaled_rf_input = scaler_rf.transform(features)

# === RF prediction ONLY ===
rf_proba = rf.predict_proba(scaled_rf_input)[:, 1][0]
rf_pred = rf_proba > 0.5

# === Output result ===
result = {
    "isFraud": bool(rf_pred),
    "probability": float(rf_proba)
}
print(json.dumps(result)) 