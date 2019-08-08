import React, { Component } from 'react'
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import Paper  from '@material-ui/core/Paper';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import './Header.css'

const initialState = {
    name:""
}

export default class Header extends Component {

    state = {...initialState}
    

    constructor(props, context){
        super(props, context);
        this.handleButtonClick = this.handleButtonClick.bind(this);
      }

    handleButtonClick(e){
        this.props.onFilterSelected(e)
    }

    handleChange(e){
        this.setState({
            name: e.target.value
        })
    }

    submit(e){
        this.props.onSubmit(this.state.name);
        this.setState({...initialState})
    }
    keyPress(e){
        if(e.keyCode === 13){
            this.props.onSubmit(this.state.name);
            this.setState({...initialState})
        }
    }
    
    render() {
        return (
            <div>
                <h1 className = "title">Gerenciador de Tarefas</h1>
                <Paper className = 'root' >
                <InputBase
                    className = 'input'
                    placeholder="O que precisa ser Feito?"
                    inputProps={{ 'aria-label': 'search google maps' }}
                    value = {this.state.name}
                    onChange = {e => {this.handleChange(e)}} onKeyDown={e => {this.keyPress(e)}}/>
                    <IconButton onClick = {e =>{this.submit(e)}}   className = 'iconButton' aria-label="search">
                         <AddIcon/>
                    </IconButton>
                </Paper>
                <Paper className = 'rootb' >
                    <ButtonGroup  variant = 'contained' color = 'primary' fullWidth>
                        <Button onClick = {e =>{this.handleButtonClick("todas")}} value = "todas">Todas</Button >
                        <Button onClick = {e =>{this.handleButtonClick( "Pendente")}}  value = "Pendente">Pendentes</Button >
                        <Button onClick = {e =>{this.handleButtonClick("Concluída")}}  value = "Concluida">Concluídas</Button >
                    </ButtonGroup >
                </Paper>
            </div>
        )
    }
}
