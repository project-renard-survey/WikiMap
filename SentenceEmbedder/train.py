import random

from keras.preprocessing.sequence import pad_sequences
from keras.utils.np_utils import to_categorical
from keras.layers import Input, Embedding, Conv1D, MaxPooling1D, Flatten, Dense
from keras.models import Model, save_model, load_model

import numpy as np

from util import *

embedding_dim = 50
max_sequence_length = 250

np.random.seed(0)

all_data = read_csv("Reformatted.csv")

text = [row[0] for row in all_data]
category_names = [row[1] for row in all_data]

t = build_tokeniser(text)

word_index = t.word_index

seq = t.texts_to_sequences(text)
labels = get_category_indeces(category_names)

data = pad_sequences(seq, maxlen=max_sequence_length)
print(data)
labels = to_categorical(np.asarray(labels))

print(data.shape)
print(labels.shape)

# split data into training and validation
indexes = np.arange(data.shape[0])
np.random.shuffle(indexes)
data = data[indexes]
labels = labels[indexes]

nb_validation_samples = int(0.2 * data.shape[0])

x_train = data[:-nb_validation_samples]
y_train = labels[:-nb_validation_samples]
x_val = data[-nb_validation_samples:]
y_val = labels[-nb_validation_samples:]

def build_model():
    # prepare embeddings stuff
    embeddings_index = {}

    print("Loading word embeddings...")

    f = open("glove.6B." + str(embedding_dim) + "d.txt", encoding="utf8")
    for line in f:
        values = line.split()
        word = values[0]
        coefs = np.asarray(values[1:], dtype="float32")
        embeddings_index[word] = coefs
    f.close()

    # compute embedding matrix
    embedding_matrix = np.zeros((len(word_index) + 1, embedding_dim))
    for word, i in word_index.items():
        embedding_vector = embeddings_index.get(word)
        if embedding_vector is not None:
            # words not found in embedding index will be all-zeros.
            embedding_matrix[i] = embedding_vector
    print("Found %s word vectors" % len(embeddings_index))

    # build the neural network

    embedding_layer = Embedding(len(word_index) + 1,
                                embedding_dim,
                                weights=[embedding_matrix],
                                input_length=max_sequence_length)

    input_tensor = Input(shape=(max_sequence_length,), dtype="int32")

    embedded_sequences = embedding_layer(input_tensor)
    x = Conv1D(128, 5, activation='relu')(embedded_sequences)
    x = MaxPooling1D(5)(x)
    x = Conv1D(128, 5, activation='relu')(x)
    x = MaxPooling1D(5)(x)
    x = Flatten()(x)
    x = Dense(128, activation='relu')(x)

    preds = Dense(labels.shape[1], activation="softmax")(x)

    model = Model(input_tensor, preds)
    model.compile(loss="categorical_crossentropy", optimizer="rmsprop", metrics=["acc"])
    return model


def log_results(filename, model:Model, epochs, batch_size, x_val, y_val):
    with open(filename, "a") as logger:
        logger.write("Model summary:\n" + model.summary() + "\n")
        logger.write("Epochs trained: " + epochs)
        logger.write("Batch size: " + batch_size)
        model.evaluate(x_val, y_val, verbose=False)

# model = build_model()
model = load_model("model.h5")
model.load_weights("weights.h5")

EPOCHS = 1
BATCH_SIZE = 2000

model.fit(x_train, y_train, validation_data=(x_val, y_val),
          epochs=EPOCHS, batch_size=BATCH_SIZE)

save_model(model, "model.h5")
model.save_weights("weights.h5")

# log_results("results_log.txt", model, epochs=EPOCHS, batch_size=BATCH_SIZE, x_val=x_val, y_val=y_val)

