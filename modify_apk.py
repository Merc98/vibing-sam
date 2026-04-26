import os
import subprocess
import shutil

def modify_apk(apk_path, command):
    if not os.path.exists(apk_path):
        print(f"Error: El archivo {apk_path} no existe.")
        return

    if "violeta" in command.lower():
        print(f"Modificando {apk_path} a color violeta...")
        subprocess.run(["apktool", "d", apk_path, "-o", "temp_apk"], check=True)
        colors_file = os.path.join("temp_apk", "res", "values", "colors.xml")
        if os.path.exists(colors_file):
            with open(colors_file, "r") as f:
                content = f.read()
            content = content.replace("#FF0000", "#9C27B0")
            with open(colors_file, "w") as f:
                f.write(content)
        subprocess.run(["apktool", "b", "temp_apk", "-o", "modified.apk"], check=True)
        print("APK modificado: modified.apk")

        # Sugerir scripts de Frida
        print("\nSugerencia de script de Frida para inyección:")
        print('''
Java.perform(function () {
  var Activity = Java.use("com.example.MainActivity");
  Activity.onCreate.save = Activity.onCreate;
  Activity.onCreate.implementation = function (bundle) {
    console.log("MainActivity.onCreate llamado");
    this.onCreate.save(bundle);
    // Cambia el color de fondo a violeta
    var rootView = this.getWindow().getDecorView().getRootView();
    rootView.setBackgroundColor(0xFF9C27B0);
  };
});
''')

        # Sugerir análisis con Ghidra
        print("\nSugerencia para análisis con Ghidra:")
        print("1. Abre Ghidra y crea un nuevo proyecto.")
        print("2. Importa el APK modificado (modified.apk).")
        print("3. Analiza el código descompilado para revisar los cambios.")

        # Analizar con jadx
        if shutil.which("jadx"):
            subprocess.run(["jadx", "modified.apk", "-d", "jadx_output"], check=True)
            print("\nAnálisis estático con jadx completado. Revisa la carpeta jadx_output.")
    else:
        print("Comando no reconocido.")

if __name__ == "__main__":
    modify_apk("example.apk", "Quiero que la app sea violeta")