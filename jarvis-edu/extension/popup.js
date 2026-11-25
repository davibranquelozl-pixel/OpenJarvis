document.getElementById("import").addEventListener("click",async()=>{
 const status=document.getElementById("s");status.textContent="Lendo seleção...";
 try{
  const [tab]=await chrome.tabs.query({active:true,currentWindow:true});
  if(!tab?.id)throw new Error("Nenhuma aba ativa.");
  const [{result:text}]=await chrome.scripting.executeScript({target:{tabId:tab.id},func:()=>window.getSelection()?.toString()||""});
  if(!text.trim())throw new Error("Selecione o texto da atividade na página primeiro.");
  const r=await fetch("http://localhost:8787/api/import",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({text})});
  if(!r.ok)throw new Error("Não foi possível conectar ao Jarvis EDU local.");
  await chrome.tabs.create({url:"http://localhost:8787"});
  status.textContent="Texto importado. Confira no Jarvis EDU.";
 }catch(e){status.textContent=e.message}
});
