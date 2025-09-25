package repository

import (
	"event_social_platform/internal/models"
	"sync"
	"time"
)

type UserRepository struct {
	users  map[uint]*models.User
	mu     sync.RWMutex
	nextID uint
}

func NewUserRepository() *UserRepository {
	return &UserRepository{
		users:  make(map[uint]*models.User),
		nextID: 1,
	}
}

func (r *UserRepository) CreateUser(user *models.User) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	user.ID = r.nextID
	user.CreatedAt = time.Now()
	r.users[user.ID] = user
	r.nextID++

	return nil
}

func (r *UserRepository) GetUserByID(id uint) (*models.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	user, exists := r.users[id]
	if !exists {
		return nil, nil
	}
	return user, nil
}

func (r *UserRepository) GetAllUsers() ([]*models.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	users := make([]*models.User, 0, len(r.users))
	for _, user := range r.users {
		users = append(users, user)
	}
	return users, nil
}

func (r *UserRepository) UserExists(email string) bool {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for _, user := range r.users {
		if user.Email == email {
			return true
		}
	}
	return false
}
