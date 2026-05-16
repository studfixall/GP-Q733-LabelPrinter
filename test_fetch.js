fetch('https://skillhub.cn/install/skillhub.md')
  .then(r => r.text())
  .then(t => console.log(t.substring(0, 1000)))
  .catch(e => console.log('Error:', e.message));
