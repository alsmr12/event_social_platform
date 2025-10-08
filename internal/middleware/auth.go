package middleware

import (
	"event_social_platform/internal/repository"
	"time"
	"github.com/gin-gonic/gin" // ← Добавьте если нет
)

func AuthMiddleware(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Пропускаем публичные маршруты
		publicRoutes := []string{"/login", "/create-profile", "/"}
		for _, route := range publicRoutes {
			if c.Request.URL.Path == route {
				c.Next()
				return
			}
		}

		// Проверяем куку с токеном
		token, err := c.Cookie("session_token")
		if err != nil {
			c.Redirect(302, "/login") // ← ИЗМЕНИТЕ: перенаправляем на логин
			c.Abort()
			return
		}

		// Ищем сессию в базе
		session, err := sessionRepo.GetSessionByToken(token)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(302, "/login") // ← ИЗМЕНИТЕ: перенаправляем на логин
			c.Abort()
			return
		}

		// Проверяем не просрочена ли сессия
		if time.Now().After(session.ExpiresAt) {
			sessionRepo.DeleteSession(token)
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(302, "/login") // ← ИЗМЕНИТЕ: перенаправляем на логин
			c.Abort()
			return
		}

		// Получаем пользователя
		user, err := userRepo.GetUserByID(session.UserID)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(302, "/login") // ← ИЗМЕНИТЕ: перенаправляем на логин
			c.Abort()
			return
		}

		c.Set("is_authenticated", true)
		c.Set("user", user)
		c.Set("user_id", user.ID)
		c.Set("CurrentUser", user)
		c.Next()
	}
}