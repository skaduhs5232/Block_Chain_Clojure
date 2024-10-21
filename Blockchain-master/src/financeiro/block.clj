(ns financeiro.block
  (:require [clojure.data.json :as json]
            [financeiro.utils :refer [formatar-bloco]]
            [clojure.string :as str]))

(defn sha256 [data]
  (let [message-digest (java.security.MessageDigest/getInstance "SHA-256")]
    (->> (.digest message-digest (.getBytes data))
         (map #(format "%02x" %))
         (apply str))))

(defn novo-bloco [numero nonce dados hash-anterior]
  (let [dados-str (json/write-str dados :escape-slash false)]
    {:numero numero
     :nonce nonce
     :dados dados-str
     :hash-anterior hash-anterior
     :hash (sha256 (str numero nonce dados-str hash-anterior))}))

(defn bloco-valido? [bloco]
  (= (subs (:hash bloco) 0 4) "0000"))

(defn encontrar-nonce [numero dados hash-anterior]
  (loop [nonce 0]
    (let [bloco (novo-bloco numero nonce dados hash-anterior)]
      (if (bloco-valido? bloco)
        nonce
        (recur (inc nonce))))))

(def blockchain (atom [{:numero 0
                        :nonce 0
                        :dados []
                        :hash-anterior "00000000000000000000000000000000"
                        :hash "0000"}]))

(defn exibir-blockchain []
  (doseq [bloco @blockchain]
    (println (formatar-bloco bloco))
    (println)))

(defn adicionar-bloco [transacao valor]
  (swap! blockchain
         (fn [chain]
           (let [ultimo-bloco (last chain)
                 numero (inc (:numero ultimo-bloco))
                 hash-anterior (:hash ultimo-bloco)
                 dados {:transacao transacao :valor valor}
                 nonce (encontrar-nonce numero dados hash-anterior)
                 novo (novo-bloco numero nonce dados hash-anterior)]
             (conj chain novo)))))

(defn adicionar-transacao [transacao valor]
  (adicionar-bloco transacao valor))

(defn adicionar-transacoes-em-bloco [transacoes]
  (let [ultimo-bloco (last @blockchain)
        numero (inc (:numero ultimo-bloco))
        hash-anterior (:hash ultimo-bloco)
        dados {:transacoes transacoes}
        nonce (encontrar-nonce numero dados hash-anterior)
        novo-bloco (novo-bloco numero nonce dados hash-anterior)]
    (swap! blockchain conj novo-bloco)))
