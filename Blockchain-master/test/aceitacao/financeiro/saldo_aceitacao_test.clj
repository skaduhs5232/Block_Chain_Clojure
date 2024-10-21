(ns financeiro.saldo-aceitacao-test
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [financeiro.auxiliares :refer [conteudo conteudo-como-json despesa
                                           endereco-para iniciar-servidor
                                           parar-servidor porta-padrao receita]]
            [financeiro.db :as db]
            [midje.parsing.1-to-explicit-form.parse-background :refer [after
                                                                       before]]
            [midje.parsing.arrow-symbols :refer [=>]]
            [midje.sweet :refer [against-background fact]]))

(against-background [(before :facts [(iniciar-servidor
                                       porta-padrao)
                                     (db/limpar)])
                     (after :facts (parar-servidor))]
  (fact "O saldo inicial é 0" :aceitacao
    (json/parse-string (conteudo "/saldo") true) => {:saldo 0})

  (fact "O saldo é 10 quando a única transação é uma receita de 10"
  :aceitacao

    (http/post (endereco-para "/transacoes")
               {:content-type :json
                :body (json/generate-string {:valor 10
                                             :tipo "receita"})})

    (json/parse-string (conteudo "/saldo") true) => {:saldo 10})

  (fact
    "O saldo é 1000 quando criamos duas receitas de 2000 e uma despesa da 3000" :aceitacao
    (http/post (endereco-para "/transacoes") (receita 2000))
    (http/post (endereco-para "/transacoes") (receita 2000))
    (http/post (endereco-para "/transacoes") (despesa 3000))

    (json/parse-string (conteudo "/saldo") true) => {:saldo 1000})

    (fact "Rejeita uma transação sem valor" :aceitacao
    #_{:clj-kondo/ignore [:unused-value]}
    (let [resposta (http/post (endereco-para "/transacoes")
                              (conteudo-como-json {:tipo
                                                     "receita"}))]
      (:status resposta) => 422))

  (fact "Rejeita uma transação com valor negativo" :aceitacao
    (let [resposta (http/post (endereco-para "/transacoes")
                              (receita -100))]
      (:status resposta) => 422))

  (fact "Rejeita uma transação com valor que não é um número"
    :aceitacao

    (let [resposta (http/post (endereco-para "/transacoes")
                              (receita "mil"))]
      (:status resposta) => 422))

  (fact "Rejeita uma transação sem tipo" :aceitacao
    (let [resposta (http/post (endereco-para "/transacoes")
                              (conteudo-como-json {:valor 70}))]
      (:status resposta) => 422))

  (fact "Rejeita uma transação com tipo desconhecido" :aceitacao
    (let [resposta (http/post (endereco-para "/transacoes")
                              (conteudo-como-json
                                {:valor 70
                                :tipo "investimento"}))]
      (:status resposta) => 422)))
