(ns financeiro.core
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.string :as str]
            [financeiro.utils :refer [formatar-bloco]]
            [financeiro.block :refer [adicionar-transacoes-em-bloco exibir-blockchain]]))

(defn format-transacoes [transacoes]
  (str/join "\n"
            (map (fn [t] (str " - Valor: " (:valor t) ", Tipo: " (:tipo t)))
                 transacoes)))

(defn opcao1 []
  (println "Opção 1 selecionada")
  (let [response (client/get "http://localhost:3000/saldo")
        body (:body response)
        sanitized-body (str/replace body #"[{}\[\]\"]" " ")]
    (println "Resposta da API para opção 1:" sanitized-body)))

(defn opcao2 []
  (println "Insira o valor da transação:")
  (let [valor (read-line)]
    (println "Insira o tipo da transação (despesa/receita):")
    (let [tipo (read-line)
          body {:valor (Double/parseDouble valor)
                :tipo tipo}
          response (client/post "http://localhost:3000/transacoes"
                                {:body (json/generate-string body)
                                 :headers {"Content-Type" "application/json"}
                                 :content-type :json})]
      (println "Resposta da API para opção 2:" (:body response)))))

(defn opcao3 []
  (let [response (client/get "http://localhost:3000/transacoes")
        body (:body response)
        parsed-body (json/parse-string body true)
        transacoes (:transacoes parsed-body)
        formatted-transacoes (format-transacoes transacoes)]
    (println "Resposta da API para opção 3:\n" formatted-transacoes)))

(defn opcao4 []
  (try
    (let [response (client/get "http://localhost:3000/transacoes")
          body (:body response)
          parsed-body (json/parse-string body true)
          transacoes (:transacoes parsed-body)]
      (adicionar-transacoes-em-bloco transacoes)
      (println "Todas as transações foram adicionadas à blockchain em um único bloco."))
    (catch Exception e
      (println "Erro ao obter transações:" (.getMessage e)))))

(defn opcao5 []
  (try
    (exibir-blockchain)
    (catch Exception e
      (println "Erro ao exibir a blockchain:" (.getMessage e)))))

(defn opcao6 []
  (println "Saindo do programa.")
  (System/exit 0))

(defn mostrar-menu []
  (println "\nMenu:")
  (println "1. Saldo")
  (println "2. Cadastrar transação")
  (println "3. Exibir transação")
  (println "4. Transformar as transações em blockchain")
  (println "5. Exibir blockchain")
  (println "6. Sair"))

(defn handle-escolha [escolha]
  (cond
    (= escolha "1") (opcao1)
    (= escolha "2") (opcao2)
    (= escolha "3") (opcao3)
    (= escolha "4") (opcao4)
    (= escolha "5") (opcao5)
    (= escolha "6") (opcao6)
    :else (println "Opção inválida. Tente novamente.")))

(defn -main [& args]
  (loop []
    (mostrar-menu)
    (let [escolha (read-line)]
      (handle-escolha escolha)
      (when (not= escolha "6")
        (recur)))))
