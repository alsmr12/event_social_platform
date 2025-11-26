package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
	"strconv"
	"time"
)

type UserHandler struct {
	userRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{userRepo: userRepo}
}

func (h *UserHandler) ShowHomePage(c *gin.Context) {
	currentUser := GetUserFromContext(c)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Главная",
		"NavActive":   "home",
		"CurrentUser": currentUser,
	})
}

func (h *UserHandler) ShowCreateProfileForm(c *gin.Context) {
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":     "Регистрация",
		"NavActive": "register",
	})
}

func (h *UserHandler) CreateProfile(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Неверные данные формы: " + err.Error(),
		})
		return
	}

	// Конвертация строки даты в time.Time
	var birthDate time.Time
	if req.BirthDate != "" {
		var err error
		birthDate, err = time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			c.HTML(http.StatusBadRequest, "base.html", gin.H{
				"Title":     "Регистрация",
				"NavActive": "register",
				"Error":     "Неверный формат даты рождения: " + err.Error(),
			})
			return
		}
	}

	if h.userRepo.UserExists(req.Email) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Пользователь с таким email уже существует",
		})
		return
	}

	user := &models.User{
		Email:     req.Email,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		Gender:    req.Gender,
		BirthDate: birthDate,
		Phone:     req.Phone,
		City:      req.City,
		Latitude:  req.Latitude,
		Longitude: req.Longitude,
		//SocialLinks: req.SocialLinks,
	}

	if err := user.HashPassword(req.Password); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Ошибка при хешировании пароля: " + err.Error(),
		})
		return
	}

	if err := h.userRepo.CreateUser(user); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Регистрация",
			"NavActive": "register",
			"Error":     "Ошибка при создании профиля: " + err.Error(),
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/login")
}

func (h *UserHandler) GetProfile(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Профиль",
			"NavActive": "profile", // ИСПРАВЛЕНО (было "profiles")
			"Error":     "Неверный ID профиля",
		})
		return
	}

	user, err := h.userRepo.GetUserByID(uint(id))
	if err != nil || user == nil {
		c.HTML(http.StatusOK, "base.html", gin.H{
			"Title":     "Профиль",
			"NavActive": "profile", // ИСПРАВЛЕНО (было "profiles")
			"Error":     "Профиль не найден",
		})
		return
	}

	// Получаем записи на стене пользователя
	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, err := wallRepo.GetPostsByUserID(uint(id))
	if err != nil {
		posts = []*models.WallPost{}
	}

	// Получаем социальные сети пользователя
	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, err := socialRepo.GetByUserID(uint(id))
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	currentUser := GetUserFromContext(c)

	// Получаем статистику подписок
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(uint(id))
	followingCount, _ := subscriptionRepo.GetFollowingCount(uint(id))

	// Проверяем, подписан ли текущий пользователь на этого пользователя
	var isSubscribed bool
	if currentUser != nil {
		isSubscribed, _ = subscriptionRepo.IsSubscribed(currentUser.ID, uint(id))
	}

	// Получаем статус дружбы
	var friendshipStatus string = "none"
	var isIncomingRequest bool = false
	if currentUser != nil && currentUser.ID != uint(id) {
		friendshipRepo := repository.NewFriendshipRepository(db)
		friendshipStatus, err = friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(id))
		if err != nil {
			friendshipStatus = "none"
		}
		if friendshipStatus == "rejected" {
			friendshipStatus = "none"
		}

		// Проверяем, является ли запрос входящим
		if friendshipStatus == "pending" {
			var friendship models.Friendship
			err := db.Where("user_id = ? AND friend_id = ?", uint(id), currentUser.ID).First(&friendship).Error
			isIncomingRequest = (err == nil)
		}
	}

	// Получаем количество друзей
	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(uint(id))

	age := user.GetAge()
	ageText := getAgeText(age)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":             "Профиль " + user.FirstName + " " + user.LastName,
		"NavActive":         "profile",
		"CurrentUser":       currentUser,
		"User":              user,
		"Posts":             posts,
		"SocialLinks":       socialLinks,
		"FollowersCount":    followersCount,
		"FollowingCount":    followingCount,
		"FriendsCount":      friendsCount,
		"IsSubscribed":      isSubscribed,
		"FriendshipStatus":  friendshipStatus,
		"IsIncomingRequest": isIncomingRequest,
		"Age":               age,
		"AgeText":           ageText,
	})
}

func (h *UserHandler) ShowEditProfileForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	db := h.userRepo.GetDB()
	socialRepo := repository.NewSocialLinkRepository(db)
	socialLinks, err := socialRepo.GetByUserID(currentUser.ID)
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Редактирование профиля",
		"NavActive":   "edit_profile",
		"User":        currentUser,
		"CurrentUser": currentUser,
		"SocialLinks": socialLinks,
	})
}

func (h *UserHandler) UpdateProfile(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.UpdateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Неверные данные формы: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	// Конвертация строки даты в time.Time
	if req.BirthDate != "" {
		birthDate, err := time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			c.HTML(http.StatusBadRequest, "base.html", gin.H{
				"Title":       "Редактирование профиля",
				"NavActive":   "profile",
				"Error":       "Неверный формат даты рождения: " + err.Error(),
				"User":        currentUser,
				"CurrentUser": currentUser,
			})
			return
		}
		currentUser.BirthDate = birthDate
	}

	// Обновляем остальные данные пользователя
	currentUser.FirstName = req.FirstName
	currentUser.LastName = req.LastName
	currentUser.Gender = req.Gender
	currentUser.Phone = req.Phone
	currentUser.City = req.City
	currentUser.Latitude = req.Latitude
	currentUser.Longitude = req.Longitude
	//currentUser.SocialLinks = req.SocialLinks

	if err := h.userRepo.UpdateUser(currentUser); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Ошибка обновления профиля: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	db := h.userRepo.GetDB()
	socialRepo := repository.NewSocialLinkRepository(db)

	// Удаляем старые соцсети
	if err := socialRepo.DeleteByUserID(currentUser.ID); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Редактирование профиля",
			"NavActive":   "profile",
			"Error":       "Ошибка обновления социальных сетей: " + err.Error(),
			"User":        currentUser,
			"CurrentUser": currentUser,
		})
		return
	}

	// Сохраняем новые соцсети
	platforms := c.PostFormArray("platform[]")
	usernames := c.PostFormArray("username[]")
	customNames := c.PostFormArray("custom_name[]")

	for i, platform := range platforms {
		if platform != "" && i < len(usernames) && usernames[i] != "" {
			socialLink := &models.SocialLink{
				UserID:     currentUser.ID,
				Platform:   platform,
				Username:   usernames[i],
				CustomName: customNames[i],
			}
			if err := socialRepo.Create(socialLink); err != nil {
				c.HTML(http.StatusInternalServerError, "base.html", gin.H{
					"Title":       "Редактирование профиля",
					"NavActive":   "profile",
					"Error":       "Ошибка сохранения социальных сетей: " + err.Error(),
					"User":        currentUser,
					"CurrentUser": currentUser,
				})
				return
			}
		}
	}
	// === КОНЕЦ ОБРАБОТКИ СОЦСЕТЕЙ ===

	c.Redirect(http.StatusSeeOther, "/profile")
}

func (h *UserHandler) GetAllProfilesJSON(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка получения пользователей",
		})
		return
	}

	// Добавляем статистику для каждого пользователя
	for _, user := range users {
		// Получаем количество подписчиков
		subscriptionRepo := repository.NewSubscriptionRepository(h.userRepo.GetDB())
		followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
		user.FollowersCount = int(followersCount)

		// Получаем количество подписок
		followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)
		user.FollowingCount = int(followingCount)

		// Получаем количество друзей
		friendshipRepo := repository.NewFriendshipRepository(h.userRepo.GetDB())
		friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)
		user.FriendsCount = int(friendsCount)

		// Получаем социальные сети
		socialRepo := repository.NewSocialLinkRepository(h.userRepo.GetDB())
		socialLinks, _ := socialRepo.GetByUserID(user.ID)
		user.SocialLinks = socialLinks

		// Получаем последние посты
		wallRepo := repository.NewWallRepository(h.userRepo.GetDB())
		posts, _ := wallRepo.GetPostsByUserID(user.ID)
		user.Posts = posts

		// Добавляем возраст и текст
		age := user.GetAge()
		user.AgeText = getAgeText(age)
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"data":    users,
	})
}

// ========== ANDROID/JSON API METHODS ==========

// UpdateProfileJSON - обновление профиля пользователя
func (h *UserHandler) UpdateProfileJSON(c *gin.Context) {
	log.Printf("🔍 UpdateProfileJSON called")

	// Получаем текущего пользователя
	user := GetUserFromContext(c)
	if user == nil {
		log.Printf("❌ UpdateProfileJSON: User not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	log.Printf("✅ UpdateProfileJSON: Updating user %s (ID: %d)", user.Email, user.ID)

	var req struct {
		FirstName   string               `json:"first_name" binding:"required"`
		LastName    string               `json:"last_name" binding:"required"`
		Gender      string               `json:"gender"`
		BirthDate   string               `json:"birth_date"`
		Phone       string               `json:"phone"`
		SocialLinks []*models.SocialLink `json:"social_links"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		log.Printf("❌ UpdateProfileJSON: Invalid request data: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Неверные данные: " + err.Error(),
		})
		return
	}

	// Обновляем данные пользователя
	user.FirstName = req.FirstName
	user.LastName = req.LastName
	user.Gender = req.Gender
	user.Phone = req.Phone

	// Обрабатываем дату рождения
	if req.BirthDate != "" {
		birthDate, err := time.Parse("2006-01-02", req.BirthDate)
		if err != nil {
			log.Printf("❌ UpdateProfileJSON: Invalid birth date: %v", err)
			c.JSON(http.StatusBadRequest, gin.H{
				"success": false,
				"message": "Неверный формат даты рождения",
			})
			return
		}
		user.BirthDate = birthDate
	} else {
		// Если дата рождения не передана, устанавливаем нулевую дату
		user.BirthDate = time.Time{}
	}

	if err := h.userRepo.UpdateUser(user); err != nil {
		log.Printf("❌ UpdateProfileJSON: Error updating user: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Ошибка обновления профиля: " + err.Error(),
		})
		return
	}

	log.Printf("✅ UpdateProfileJSON: Profile updated successfully for user %s", user.Email)

	age := user.GetAge()
	ageText := getAgeText(age)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Профиль успешно обновлен",
		"user": gin.H{
			"id":         user.ID,
			"email":      user.Email,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"gender":     user.Gender,
			"birth_date": user.BirthDate.Format("2006-01-02"),
			"age":        age,
			"age_text":   ageText,
			"phone":      user.Phone,
		},
	})
}

// getAgeText возвращает правильную форму слова "год" в зависимости от возраста
func getAgeText(age int) string {
	if age == 1 || (age%10 == 1 && age%100 != 11) {
		return "год"
	} else if age >= 2 && age <= 4 || (age%10 >= 2 && age%10 <= 4 && !(age%100 >= 12 && age%100 <= 14)) {
		return "года"
	}
	return "лет"
}

// GetUserStatsJSON - получение статистики пользователя
func (h *UserHandler) GetUserStatsJSON(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"success": false,
			"message": "Не авторизован",
		})
		return
	}

	age := currentUser.GetAge()
	ageText := getAgeText(age)

	db := h.userRepo.GetDB()

	// Получаем количество друзей
	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(currentUser.ID)

	// Получаем количество подписчиков и подписок
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(currentUser.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(currentUser.ID)

	// Получаем количество событий пользователя
	eventRepo := repository.NewEventRepository(db)
	userEventsCount, _ := eventRepo.GetUserEventsCountAndroid(currentUser.ID)

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"stats": gin.H{
			"friends_count":   friendsCount,
			"followers_count": followersCount,
			"following_count": followingCount,
			"events_count":    userEventsCount,
			"age":             age,
			"age_text":        ageText,
		},
	})
}
