(ns xhub-team.errors)

(def validate-error
  {:error-data (list {:error_code 1 :error_message "Не верный формат запроса"})})

(def photo-load-error
  {:error-data (list {:error_code 2 :error_message "Произшла ошибка при загрузке фото"})})
