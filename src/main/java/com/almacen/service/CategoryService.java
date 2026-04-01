package com.almacen.service;

import com.almacen.dao.CategoryDAO;
import com.almacen.model.Category;

import java.util.List;
import java.util.Optional;

public class CategoryService {
    private final CategoryDAO categoryDAO;
    
    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }
    
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }
    
    public Optional<Category> getCategoryById(Long id) {
        return categoryDAO.findById(id);
    }
    
    public Optional<Category> getCategoryByName(String name) {
        return categoryDAO.findByName(name);
    }
    
    public Category createCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }
        
        name = name.trim();
        
        if (categoryDAO.existsByName(name)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        
        Category category = new Category(name);
        return categoryDAO.save(category);
    }
    
    public Category updateCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("Categoría inválida para actualizar");
        }
        
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }
        
        String name = category.getName().trim();
        
        // Verificar si ya existe otra categoría con ese nombre
        Optional<Category> existing = categoryDAO.findByName(name);
        if (existing.isPresent() && !existing.get().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Ya existe otra categoría con ese nombre");
        }
        
        category.setName(name);
        return categoryDAO.save(category);
    }
    
    public boolean deleteCategory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID de categoría no puede ser nulo");
        }
        
        // Aquí podríamos verificar si hay productos asociados antes de eliminar
        // Por ahora, permitimos eliminar (la base de datos manejará la integridad referencial)
        return categoryDAO.delete(id);
    }
    
    public boolean categoryExists(String name) {
        return categoryDAO.existsByName(name);
    }
}
