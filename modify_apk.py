import os
import subprocess

def modify_apk(apk_path, command):
    if "violeta" in command.lower():
        print(f"Modificando {apk_path} a color violeta...")
        os.system(f"apktool d {apk_path} -o temp_apk")
        # Modificar recursos (ej: editar colors.xml en temp_apk/res/values/)
        os.system(f"apktool b temp_apk -o modified.apk")
        print("APK modificado: modified.apk")
    else:
        print("Comando no reconocido.")

if __name__ == "__main__":
    modify_apk("example.apk", "Quiero que la app sea violeta")