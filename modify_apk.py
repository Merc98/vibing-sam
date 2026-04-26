import os
import subprocess
import shutil

def modify_apk(apk_path, command):
    if not os.path.exists(apk_path):
        print(f"Error: El archivo {apk_path} no existe.")
        return

    if "violeta" in command.lower():
        print(f"Modificando {apk_path} a color violeta...")
        
        # Descompilar el APK
        if not os.path.exists("temp_apk"):
            os.makedirs("temp_apk")
        subprocess.run(["apktool", "d", apk_path, "-o", "temp_apk"], check=True)
        
        # Modificar recursos (ejemplo: cambiar colores)
        colors_file = os.path.join("temp_apk", "res", "values", "colors.xml")
        if os.path.exists(colors_file):
            with open(colors_file, "r") as f:
                content = f.read()
            content = content.replace("#FF0000", "#9C27B0")  # Cambiar rojo a violeta
            with open(colors_file, "w") as f:
                f.write(content)
        
        # Recompilar el APK
        subprocess.run(["apktool", "b", "temp_apk", "-o", "modified.apk"], check=True)
        
        # Analizar con jadx (opcional)
        if shutil.which("jadx"):
            subprocess.run(["jadx", "modified.apk", "-d", "jadx_output"], check=True)
            print("Análisis estático con jadx completado. Ver carpeta jadx_output.")
        
        print("APK modificado: modified.apk")
    else:
        print("Comando no reconocido.")

if __name__ == "__main__":
    modify_apk("example.apk", "Quiero que la app sea violeta")