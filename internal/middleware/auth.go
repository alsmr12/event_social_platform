package middleware

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
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
			c.Redirect(http.StatusSeeOther, "/login")
			c.Abort()
			return
		}

		// Ищем сессию в базе
		session, err := sessionRepo.GetSessionByToken(token)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(http.StatusSeeOther, "/login")
			c.Abort()
			return
		}

		// Проверяем не просрочена ли сессия
		if time.Now().After(session.ExpiresAt) {
			sessionRepo.DeleteSession(token)
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(http.StatusSeeOther, "/login")
			c.Abort()
			return
		}

		// Получаем пользователя
		user, err := userRepo.GetUserByID(session.UserID)
		if err != nil {
			c.SetCookie("session_token", "", -1, "/", "", false, true)
			c.Redirect(http.StatusSeeOther, "/login")
			c.Abort()
			return
		}

		c.Set("is_authenticated", true)
		c.Set("user", user)
		c.Set("user_id", user.ID)
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
