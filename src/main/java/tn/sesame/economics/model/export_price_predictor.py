import torch
import torch.nn as nn
import numpy as np

class TunisianExportPredictor(nn.Module):
    """
    Modèle PyTorch simple pour prédire les prix d'exportation tunisiens
    Architecture: 10 features → 32 → 16 → 1 (prix)
    """
    def __init__(self):
        super(TunisianExportPredictor, self).__init__()
        self.fc1 = nn.Linear(10, 32)  # 10 caractéristiques d'entrée
        self.fc2 = nn.Linear(32, 16)
        self.fc3 = nn.Linear(16, 1)
        self.relu = nn.ReLU()
        self.dropout = nn.Dropout(0.2)

    def forward(self, x):
        x = self.relu(self.fc1(x))
        x = self.dropout(x)
        x = self.relu(self.fc2(x))
        x = self.fc3(x)
        return x

# Créer et sauvegarder le modèle
if __name__ == "__main__":
    # Créer le modèle
    model = TunisianExportPredictor()

    # Créer des poids aléatoires (dans un vrai cas, on entraînerait)
    torch.save(model.state_dict(), "tunisian_export_model.pth")

    # Exporter en TorchScript pour DJL
    example_input = torch.randn(1, 10)  # Exemple d'entrée
    traced_model = torch.jit.trace(model, example_input)
    traced_model.save("tunisian_export_model_traced.pt")

    print("✅ Modèle PyTorch créé et sauvegardé")
    print(f"  - Paramètres: {sum(p.numel() for p in model.parameters())}")