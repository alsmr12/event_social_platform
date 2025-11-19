package middleware

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"time"
	"log"
	"net/http"
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

// StrictAuthMiddleware - строгая проверка (редирект на логин)
func StrictAuthMiddleware(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) gin.HandlerFunc {
	return func(c *gin.Context) {
		log.Printf("🔒 StrictAuthMiddleware: checking strict auth for path: %s", c.Request.URL.Path)
		log.Printf("🌐 StrictAuthMiddleware: request from: %s", c.Request.RemoteAddr)
		log.Printf("🔗 StrictAuthMiddleware: user agent: %s", c.Request.UserAgent())
		
		// Логируем ВСЕ заголовки
		log.Printf("📋 StrictAuthMiddleware: ALL HEADERS:")
		for name, values := range c.Request.Header {
			for _, value := range values {
				log.Printf("   %s: %s", name, value)
			}
		}

		// Логируем куки
		cookies := c.Request.Cookies()
		if len(cookies) > 0 {
			log.Printf("🍪 StrictAuthMiddleware: received cookies:")
			for _, cookie := range cookies {
				log.Printf("   - %s: %s (Domain: %s, Path: %s)", cookie.Name, cookie.Value, cookie.Domain, cookie.Path)
			}
		} else {
			log.Printf("❌ StrictAuthMiddleware: NO COOKIES received at all!")
		}

		token, err := c.Cookie("session_token")
		if err != nil {
			log.Printf("❌ StrictAuthMiddleware: no session_token cookie found: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Не авторизован",
			})
			c.Abort()
			return
		}

		log.Printf("🔑 StrictAuthMiddleware: found session_token: %s", token)
		
		session, err := sessionRepo.GetSessionByToken(token)
		if err != nil {
			log.Printf("❌ StrictAuthMiddleware: session not found or expired: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Сессия истекла",
			})
			c.Abort()
			return
		}

		user, err := userRepo.GetUserByID(session.UserID)
		if err != nil {
			log.Printf("❌ StrictAuthMiddleware: user not found for session: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Пользователь не найден",
			})
			c.Abort()
			return
		}

		log.Printf("✅ StrictAuthMiddleware: user authenticated: %s (ID: %d)", user.Email, user.ID)
		c.Set("user", user)
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
