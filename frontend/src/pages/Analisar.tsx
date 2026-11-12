import { useState } from "react";
import { useDropzone } from "react-dropzone";
import { Link } from "react-router-dom";
import styles from "./Pages.module.css";

export default function Analisar() {
  const [arquivo, setArquivo] = useState<File | null>(null);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    accept: { "application/pdf": [".pdf"] },
    maxFiles: 1,
    onDrop: (files) => {
      setArquivo(files[0] ?? null);
      // TODO: enviar para o backend: POST /api/extrato (multipart/form-data)
    },
  });

  return (
    <main className={styles.center}>
      <h1>Anexe seu extrato</h1>
      <div {...getRootProps()} className={`${styles.dropzone} ${isDragActive ? styles.active : ""}`}>
        <input {...getInputProps()} />
        {arquivo
          ? `Arquivo selecionado: ${arquivo.name}`
          : isDragActive
          ? "Solte o PDF aqui"
          : "Arraste seu extrato em PDF ou clique para escolher"}
      </div>
      <Link to="/" className={styles.link}>Voltar</Link>
    </main>
  );
}
