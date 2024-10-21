(ns financeiro.utils
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))

(defn formatar-bloco [bloco]
  (-> (json/write-str bloco :escape-slash false)
      (str/replace #"[{}\[\]\"]" "") ; Remove chaves, colchetes e aspas
      (str/replace #"," "\n")        ; Substitui vírgulas por quebras de linha
      (str/replace #":" ": ")))      ; Adiciona espaço após os dois-pontos
