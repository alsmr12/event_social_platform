package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type WallRepository struct {
	db *gorm.DB
}

func NewWallRepository(db *gorm.DB) *WallRepository {
	return &WallRepository{db: db}
}

func (r *WallRepository) CreatePost(post *models.WallPost) error {
	return r.db.Create(post).Error
}

func (r *WallRepository) GetPostsByUserID(userID uint) ([]*models.WallPost, error) {
	var posts []*models.WallPost
	err := r.db.Preload("Author").Where("user_id = ?", userID).Order("created_at DESC").Find(&posts).Error
	if err != nil {
		return nil, err
	}
	return posts, nil
}

func (r *WallRepository) GetPostByID(id uint) (*models.WallPost, error) {
	var post models.WallPost
	err := r.db.Preload("Author").Preload("User").First(&post, id).Error
	if err != nil {
		return nil, err
	}
	return &post, nil
}

func (r *WallRepository) DeletePost(id uint) error {
	return r.db.Delete(&models.WallPost{}, id).Error
}

func (r *WallRepository) CanDeletePost(postID uint, userID uint) (bool, error) {
	var post models.WallPost
	err := r.db.First(&post, postID).Error
	if err != nil {
		return false, err
	}

	// Может удалить автор записи или владелец стены
	return post.AuthorID == userID || post.UserID == userID, nil
}

// / Редактирование
// Добавляем метод для обновления записи
func (r *WallRepository) UpdatePost(post *models.WallPost) error {
	return r.db.Save(post).Error
}

// Добавляем проверку прав на редактирование (может редактировать только автор)
func (r *WallRepository) CanEditPost(postID uint, userID uint) (bool, error) {
	var post models.WallPost
	err := r.db.First(&post, postID).Error
	if err != nil {
		return false, err
	}

	// Может редактировать только автор записи
	return post.AuthorID == userID, nil
}
