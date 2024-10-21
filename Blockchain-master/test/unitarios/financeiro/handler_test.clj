(ns financeiro.handler-test
  (:require [cheshire.core :as json]
            [financeiro.db :as db]
            [financeiro.handler :refer [app]]
            [midje.parsing.arrow-symbols :refer [=>]]
            [midje.sweet :refer [against-background fact facts]]
            [ring.mock.request :as mock]))

(facts "Dá um 'Olá, mundo!' na rota raiz"
  (let [response (app (mock/request :get "/"))]
    (fact "o status da reposta é 200"
      (:status response) => 200)

    (fact "o texto do corpo é 'Olá, mundo!'"
      (:body response) => "Olá, mundo!")))

(facts "Rota inválida não existe"
  (let [response (app (mock/request :get "/invalid"))]
    (fact "o código de erro é 404"
      (:status response) => 404)

    (fact "o texto do corpo é 'Recurso não encontrado'"
      (:body response) => "Recurso não encontrado")))

(facts "Saldo inicial é 0"
  (against-background [(json/generate-string {:saldo 0})
                         => "{\"saldo\":0}"
                       (db/saldo) => 0])

  (let [response (app (mock/request :get "/saldo"))]
    (fact "o formato é 'application/json'"
      (get-in response [:headers "Content-Type"])
        => "application/json; charset=utf-8")

    (fact "o status da reposta é 200"
      (:status response) => 200)

    (fact
      "o texto do corpo é um JSON cuja chave é saldo e o valor é 0"
      (:body response) => "{\"saldo\":0}")))

(facts "Registra uma receita no valor de 10"
       (against-background (db/registrar {:valor 10
                                          :tipo "receita"})
         => {:id 1 :valor 10 :tipo "receita"})

       (let [response
               (app (-> (mock/request :post "/transacoes")
                        (mock/json-body {:valor 10
                                         :tipo "receita"})))]

         (fact "o status da reposta é 201"
               (:status response) => 201)

         (fact "o texto do corpo é um JSON com o conteúdo enviado e um id"
               (:body response) =>
                 "{\"id\":1,\"valor\":10,\"tipo\":\"receita\"}")))

(facts "Existe rota para lidar com filtro de transação por tipo"
   (against-background [(db/transacoes-do-tipo "receita") => '({:id 1 :valor 2000 :tipo "receita"})
                        (db/transacoes-do-tipo "despesa") => '({:id 2 :valor 89 :tpo "despesa"})
                        (db/transacoes) => '({:id 1 :valor 2000 :tipo "receita"}
                                            {:id 2 :valor 89 :tpo "despesa"})]
     (fact "Filtro por receita"
       (let [response (app (mock/request :get "/receitas"))]
         (:status response) => 200
         (:body response) => (json/generate-string {:transacoes '({:id 1 :valor 2000 :tipo "receita"})})))

     (fact "Filtro por despesa"
       (let [response (app (mock/request :get "/despesas"))]
         (:status response) => 200
         (:body response) => (json/generate-string {:transacoes '({:id 2 :valor 89 :tpo "despesa"})})))

     (fact "Sem filtro"
       (let [response (app (mock/request :get "/transacoes"))]
         (:status response) => 200
         (:body response) => (json/generate-string {:transacoes '({:id 1 :valor 2000 :tipo "receita"}
                                                                  {:id 2 :valor 89 :tpo "despesa"})})))))

(facts "Filtra transações por parâmetros de busca na URL"
       (def livro {:id 1 :valor 88 :tipo "despesa" :rotulos ["livro" "educação"]})
       (def curso {:id 2 :valor 106 :tipo "despesa" :rotulos ["curso" "educação"]})
       (def salario {:id 3 :valor 8000 :tipo "receita" :rotulos ["salário"]})

       (against-background [(db/transacoes-com-filtro {:rotulos ["livro" "curso"]}) => [livro curso]
                            (db/transacoes-com-filtro {:rotulos "salário"}) => [salario]]
         (fact "Filtro múltiplos rótulos"
           (let [response (app (mock/request :get "/transacoes?rotulos=livro&rotulos=curso"))]
             (:status response) => 200
             (:body response) => (json/generate-string {:transacoes [livro curso]})))

         (fact "Filtro com único rótulo"
           (let [response (app (mock/request :get "/transacoes?rotulos=salário"))]
             (:status response) => 200
             (:body response) => (json/generate-string {:transacoes [salario]})))))
