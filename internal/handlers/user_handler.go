package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type UserHandler struct {
	userRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{userRepo: userRepo}
}

func (h *UserHandler) ShowHomePage(c *gin.Context) {
	c.HTML(http.StatusOK, "index.html", gin.H{
		"Title": "Социальная платформа",
	})
}

func (h *UserHandler) ShowCreateProfileForm(c *gin.Context) {
	c.HTML(http.StatusOK, "create_profile.html", gin.H{})
}

func (h *UserHandler) CreateProfile(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Неверные данные формы",
		})
		return
	}

	if h.userRepo.UserExists(req.Email) {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Пользователь с таким email уже существует",
		})
		return
	}

	// Создаем пользователя (БЕЗ пароля пока)
	user := &models.User{
		Email:       req.Email,
		FirstName:   req.FirstName,
		LastName:    req.LastName,
		Gender:      req.Gender,
		Age:         req.Age,
		Phone:       req.Phone,
		SocialLinks: req.SocialLinks,
	}

	// Хешируем пароль!
	if err := user.HashPassword(req.Password); err != nil {
		c.HTML(http.StatusInternalServerError, "create_profile.html", gin.H{
			"Error": "Ошибка при хешировании пароля: " + err.Error(),
		})
		return
	}

	// Сохраняем в БД
	if err := h.userRepo.CreateUser(user); err != nil {
		c.HTML(http.StatusInternalServerError, "create_profile.html", gin.H{
			"Error": "Ошибка при создании профиля: " + err.Error(),
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/login")
}

func (h *UserHandler) GetProfile(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Неверный ID профиля",
		})
		return
	}

	user, err := h.userRepo.GetUserByID(uint(id))
	if err != nil || user == nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Профиль не найден",
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

		// Проверяем, является ли запрос входящим
		if friendshipStatus == "pending" {
			var friendship models.Friendship
			err := db.Where("user_id = ? AND friend_id = ?", uint(id), currentUser.ID).First(&friendship).Error
			isIncomingRequest = (err == nil)
		}
	}

	c.HTML(http.StatusOK, "profile.html", gin.H{
		"User":              user,
		"Posts":             posts,
		"CurrentUser":       currentUser,
		"FollowersCount":    followersCount,
		"FollowingCount":    followingCount,
		"IsSubscribed":      isSubscribed,
		"FriendshipStatus":  friendshipStatus,
		"IsIncomingRequest": isIncomingRequest,
	})
}

func (h *UserHandler) GetAllProfiles(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		// Всегда возвращаем Users, даже если пустой массив
		c.HTML(http.StatusOK, "profiles.html", gin.H{
			"Users": []*models.User{},
		})
		return
	}

	// Если users nil, заменяем на пустой массив
	if users == nil {
		users = []*models.User{}
	}

	currentUser := GetUserFromContext(c)

	// Создаем структуру для передачи дополнительных данных
	type UserWithSubscriptions struct {
		*models.User
		FollowersCount int64
		FollowingCount int64
		IsSubscribed   bool
	}

	usersWithSubs := make([]UserWithSubscriptions, len(users))

	// Получаем репозиторий подписок
	db := h.userRepo.GetDB()
	subscriptionRepo := repository.NewSubscriptionRepository(db)

	for i, user := range users {
		// Получаем статистику подписок для каждого пользователя
		followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
		followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

		// Проверяем, подписан ли текущий пользователь
		var isSubscribed bool
		if currentUser != nil && currentUser.ID != user.ID {
			isSubscribed, _ = subscriptionRepo.IsSubscribed(currentUser.ID, user.ID)
		}

		usersWithSubs[i] = UserWithSubscriptions{
			User:           user,
			FollowersCount: followersCount,
			FollowingCount: followingCount,
			IsSubscribed:   isSubscribed,
		}
	}

	c.HTML(http.StatusOK, "profiles.html", gin.H{
		"Users":       usersWithSubs,
		"CurrentUser": currentUser,
	})
}

// Показывает форму редактирования профиля
func (h *UserHandler) ShowEditProfileForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	c.HTML(http.StatusOK, "edit_profile.html", gin.H{
		"User": currentUser,
	})
}

// Обрабатывает обновление профиля
func (h *UserHandler) UpdateProfile(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.UpdateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "edit_profile.html", gin.H{
			"Error": "Неверные данные формы: " + err.Error(),
			"User":  currentUser,
		})
		return
	}

	// Обновляем данные пользователя
	currentUser.FirstName = req.FirstName
	currentUser.LastName = req.LastName
	currentUser.Gender = req.Gender
	currentUser.Age = req.Age
	currentUser.Phone = req.Phone
	currentUser.SocialLinks = req.SocialLinks

	if err := h.userRepo.UpdateUser(currentUser); err != nil {
		c.HTML(http.StatusInternalServerError, "edit_profile.html", gin.H{
			"Error": "Ошибка обновления профиля: " + err.Error(),
			"User":  currentUser,
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/profile")
}
