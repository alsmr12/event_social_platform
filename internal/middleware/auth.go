package middleware

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"time"
	"log"
	"net/http"
	"strings"
)

func AuthMiddleware(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Пропускаем страницу логина без проверки
		if c.Request.URL.Path == "/login" {
			c.Next()
			return
		}

		token := getTokenFromRequest(c)

		if token == "" {
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
		log.Printf("🔒 StrictAuthMiddleware: checking auth for path: %s", c.Request.URL.Path)
		
		token := getTokenFromRequest(c)
		
		if token == "" {
			log.Printf("❌ StrictAuthMiddleware: no token found in request")
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Не авторизован",
			})
			c.Abort()
			return
		}

		log.Printf("🔑 StrictAuthMiddleware: found token: %s", token)
		
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
		c.Set("user_id", user.ID)                   
		c.Set("is_authenticated", true)
		c.Set("CurrentUser", user)
		
		c.Next()
	}
}

// Вспомогательная функция для получения токена из запроса
func getTokenFromRequest(c *gin.Context) string {
	// 1. Пробуем получить из заголовка Authorization
	authHeader := c.GetHeader("Authorization")
	if strings.HasPrefix(authHeader, "Bearer ") {
		token := strings.TrimPrefix(authHeader, "Bearer ")
		log.Printf("📨 Got token from Authorization header: %s", token)
		return token
	}

	// 2. Пробуем получить из куки
	cookieToken, err := c.Cookie("session_token")
	if err == nil && cookieToken != "" {
		log.Printf("🍪 Got token from cookie: %s", cookieToken)
		return cookieToken
	}

	// 3. Пробуем получить из query параметра (на всякий случай)
	queryToken := c.Query("token")
	if queryToken != "" {
		log.Printf("🔍 Got token from query: %s", queryToken)
		return queryToken
	}

	return ""
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