package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type AuthHandler struct {
	userRepo    *repository.UserRepository
	sessionRepo *repository.SessionRepository
}

func NewAuthHandler(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) *AuthHandler {
	return &AuthHandler{
		userRepo:    userRepo,
		sessionRepo: sessionRepo,
	}
}

// ---------- WEB METHODS (старые, для HTML) ----------

func (h *AuthHandler) ShowLoginForm(c *gin.Context) {
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":     "Вход в систему",
		"NavActive": "login",
	})
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email или пароль",
		})
		return
	}

	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email",
		})
		return
	}

	if !user.CheckPassword(req.Password) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный пароль",
		})
		return
	}

	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := h.sessionRepo.CreateSession(session); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Ошибка создания сессии",
		})
		return
	}

	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/profile")
}

func (h *AuthHandler) Logout(c *gin.Context) {
	token, err := c.Cookie("session_token")
	if err == nil {
		h.sessionRepo.DeleteSession(token)
	}
	c.SetCookie("session_token", "", -1, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/")
}

func (h *AuthHandler) ShowProfile(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, _ := wallRepo.GetPostsByUserID(user.ID)

	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, _ := socialRepo.GetByUserID(user.ID)

	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Мой профиль",
		"NavActive":      "my_profile",
		"User":           user,
		"Posts":          posts,
		"SocialLinks":    socialLinks,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
		"FriendsCount":   friendsCount,
		"CurrentUser":    user,
	})
}

// ---------- ANDROID/JSON API METHODS ----------

func (h *AuthHandler) LoginJSON(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Неверный email или пароль"})
		return
	}

	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil || !user.CheckPassword(req.Password) {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Неверный email или пароль"})
		return
	}

	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := h.sessionRepo.CreateSession(session); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Ошибка создания сессии"})
		return
	}

	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"BirthDate":  user.BirthDate,
			"phone":      user.Phone,
		},
		"token": token,
	})
}

// ProfileJSON - получение профиля пользователя
func (h *AuthHandler) ProfileJSON(c *gin.Context) {
	log.Printf("🔍 ProfileJSON called - checking authentication")

	// Вариант 1: Попробуем получить пользователя из контекста (если middleware работает)
	user := GetUserFromContext(c)

	if user == nil {
		log.Printf("❌ ProfileJSON: No user in context, trying alternative methods")

		// Вариант 2: Попробуем получить токен напрямую из запроса
		token := getTokenFromRequest(c)
		if token == "" {
			log.Printf("❌ ProfileJSON: No token found in request")
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Не авторизован: токен не найден",
			})
			return
		}

		log.Printf("🔑 ProfileJSON: Found token directly: %s", token)

		// Найдем сессию по токену
		session, err := h.sessionRepo.GetSessionByToken(token)
		if err != nil {
			log.Printf("❌ ProfileJSON: Session not found: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Сессия не найдена",
			})
			return
		}

		// Проверим, что токен не просрочен
		if time.Now().After(session.ExpiresAt) {
			log.Printf("❌ ProfileJSON: Session expired")
			h.sessionRepo.DeleteSession(token)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Сессия истекла",
			})
			return
		}

		// Получим пользователя по ID из сессии
		user, err = h.userRepo.GetUserByID(session.UserID)
		if err != nil {
			log.Printf("❌ ProfileJSON: User not found: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"success": false,
				"message": "Пользователь не найден",
			})
			return
		}

		log.Printf("✅ ProfileJSON: User found via token: %s", user.Email)
	} else {
		log.Printf("✅ ProfileJSON: User from context: %s", user.Email)
	}

	// Дальше обычная логика
	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, _ := wallRepo.GetPostsByUserID(user.ID)

	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, _ := socialRepo.GetByUserID(user.ID)

	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)

	log.Printf("✅ ProfileJSON: Returning profile for user %s", user.Email)

	c.JSON(http.StatusOK, gin.H{
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"BirthDate":  user.BirthDate,
			"phone":      user.Phone,
		},
		"posts":         posts,
		"social_links":  socialLinks,
		"followers":     followersCount,
		"following":     followingCount,
		"friends_count": friendsCount,
	})
}

func (h *AuthHandler) RegisterJSON(c *gin.Context) {
	var req models.RegisterRequest // используем существующую структуру
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверные данные: " + err.Error(),
		})
		return
	}

	// Конвертация даты
	var birthDate time.Time
	if req.BirthDate != "" {
		var err error
		birthDate, err = time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{
				"success": false,
				"message": "Неверная дата: " + err.Error(),
			})
			return
		}
	}

	// Проверка существования пользователя
	if h.userRepo.UserExists(req.Email) {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Email уже занят",
		})
		return
	}

	// Создание пользователя
	user := &models.User{
		Email:     req.Email,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		Gender:    req.Gender,
		BirthDate: birthDate,
		Phone:     req.Phone,
	}

	// Хеширование пароля
	if err := user.HashPassword(req.Password); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка пароля",
		})
		return
	}

	// Сохранение
	if err := h.userRepo.CreateUser(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка создания",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success":      true,
		"message":      "Профиль создан",
		"redirect_url": "/login",
	})
}

// getTokenFromRequest - вспомогательная функция для получения токена из запроса
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
