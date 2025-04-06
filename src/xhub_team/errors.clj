(ns xhub-team.errors)

(def validate-error
  {:error-data (list {:error_code 1 :error_message "Не верный формат запроса"})})

(def photo-load-error
  {:error-data (list {:error_code 2 :error_message "Произшла ошибка при загрузке фото"})})

(def photo-not-found
  {:error-data (list {:error_code 3 :error_message "Страница не найдена"})})

(def not-auth-user
  {:error-data (list {:error_code 4 :error_message "Пользователь не авторизован"})})

(def load-delete-permission-error
  {:error-data (list {:error_code 5 :error_message "Для загрузки изображений нужно быть автором или администратором"})})
