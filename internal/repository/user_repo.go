package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type UserRepository struct {
	db *gorm.DB
}

func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) CreateUser(user *models.User) error {
	return r.db.Create(user).Error
}

func (r *UserRepository) GetUserByID(id uint) (*models.User, error) {
	var user models.User
	err := r.db.First(&user, id).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetAllUsers() ([]*models.User, error) {
	var users []*models.User
	err := r.db.Find(&users).Error
	if err != nil {
		return nil, err
	}
	return users, nil
}

func (r *UserRepository) UserExists(email string) bool {
	var count int64
	r.db.Model(&models.User{}).Where("email = ?", email).Count(&count)
	return count > 0
}

func (r *UserRepository) GetUserByEmail(email string) (*models.User, error) {
	var user models.User
	err := r.db.Where("email = ?", email).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}
