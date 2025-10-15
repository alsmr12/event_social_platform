package middleware

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"time"
)

func AuthMiddleware(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Пропускаем страницу логина без проверки
		if c.Request.URL.Path == "/login" {
			c.Next()
			return
		}

		// Проверяем куку с токеном
		token, err := c.Cookie("session_token")
		if err != nil {
			c.Next() // Продолжаем без аутентификации
			return
		}

		// Ищем сессию в базе
		session, err := sessionRepo.GetSessionByToken(token)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Next() // Продолжаем без аутентификации
			return
		}

		// Проверяем не просрочена ли сессия
		if time.Now().After(session.ExpiresAt) {
			sessionRepo.DeleteSession(token)
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Next() // Продолжаем без аутентификации
			return
		}

		// Получаем пользователя
		user, err := userRepo.GetUserByID(session.UserID)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Next() // Продолжаем без аутентификации
			return
		}

		c.Set("is_authenticated", true)
		c.Set("user", user)
		c.Set("user_id", user.ID)
		c.Set("CurrentUser", user)
		c.Next()
	}
}

// Вспомогательные функции
func GetUserFromContext(c *gin.Context) *models.User {
	user, exists := c.Get("user")
	if !exists {
		return nil
	}
	return user.(*models.User)
}

func IsAuthenticated(c *gin.Context) bool {
	isAuth, exists := c.Get("is_authenticated")
	if !exists {
		return false
	}
	return isAuth.(bool)
}
